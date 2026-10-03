package com.gauri.splitter.util;

import com.gauri.splitter.dto.SplitInput;
import com.gauri.splitter.entity.SplitType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public final class SplitCalculator {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal ONE_PAISA = new BigDecimal("0.01");

    private SplitCalculator() {}

    /** Returns userId -> share. The shares always add up to exactly `total`. */
    public static Map<Long, BigDecimal> calculate(SplitType type, BigDecimal total, List<SplitInput> inputs) {
        if (total == null || total.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        if (inputs == null || inputs.isEmpty()) {
            throw new IllegalArgumentException("At least one person must share the expense");
        }
        long distinct = inputs.stream().map(SplitInput::userId).distinct().count();
        if (distinct != inputs.size()) {
            throw new IllegalArgumentException("A user appears more than once in the split");
        }
        total = total.setScale(2, RoundingMode.HALF_UP);

        return switch (type) {
            case EQUAL -> equal(total, inputs);
            case EXACT -> exact(total, inputs);
            case PERCENTAGE -> percentage(total, inputs);
        };
    }

    private static Map<Long, BigDecimal> equal(BigDecimal total, List<SplitInput> inputs) {
        int n = inputs.size();
        BigDecimal count = BigDecimal.valueOf(n);
        BigDecimal base = total.divide(count, 2, RoundingMode.DOWN);          // 100 / 3 = 33.33
        int leftoverPaise = total.subtract(base.multiply(count))              // 100.00 - 99.99 = 0.01
                .movePointRight(2).intValueExact();                           // = 1 paisa to hand out

        Map<Long, BigDecimal> result = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            BigDecimal share = i < leftoverPaise ? base.add(ONE_PAISA) : base; // first people get +0.01
            result.put(inputs.get(i).userId(), share);
        }
        return result;
    }

    private static Map<Long, BigDecimal> exact(BigDecimal total, List<SplitInput> inputs) {
        Map<Long, BigDecimal> result = new LinkedHashMap<>();
        BigDecimal sum = BigDecimal.ZERO;
        for (SplitInput in : inputs) {
            if (in.value() == null || in.value().signum() < 0) {
                throw new IllegalArgumentException("Each person needs a non-negative amount");
            }
            BigDecimal share = in.value().setScale(2, RoundingMode.HALF_UP);
            result.put(in.userId(), share);
            sum = sum.add(share);
        }
        if (sum.compareTo(total) != 0) {
            throw new IllegalArgumentException("Split amounts add up to " + sum + " but the expense is " + total);
        }
        return result;
    }

    private static Map<Long, BigDecimal> percentage(BigDecimal total, List<SplitInput> inputs) {
        BigDecimal pctSum = BigDecimal.ZERO;
        for (SplitInput in : inputs) {
            if (in.value() == null || in.value().signum() < 0) {
                throw new IllegalArgumentException("Each person needs a non-negative percentage");
            }
            pctSum = pctSum.add(in.value());
        }
        if (pctSum.compareTo(HUNDRED) != 0) {
            throw new IllegalArgumentException("Percentages add up to " + pctSum + ", they must add up to 100");
        }

        Map<Long, BigDecimal> result = new LinkedHashMap<>();
        BigDecimal sum = BigDecimal.ZERO;
        Long largestUser = null;
        BigDecimal largestPct = BigDecimal.valueOf(-1);

        for (SplitInput in : inputs) {
            BigDecimal share = total.multiply(in.value()).divide(HUNDRED, 2, RoundingMode.HALF_UP);
            result.put(in.userId(), share);
            sum = sum.add(share);
            if (in.value().compareTo(largestPct) > 0) {
                largestPct = in.value();
                largestUser = in.userId();
            }
        }
        // Rounding can leave us a paisa or two off: give the difference to the biggest share
        BigDecimal diff = total.subtract(sum);
        if (diff.signum() != 0) {
            result.merge(largestUser, diff, BigDecimal::add);
        }
        return result;
    }
}