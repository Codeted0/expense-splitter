package com.gauri.splitter.util;

import com.gauri.splitter.dto.SplitInput;
import com.gauri.splitter.entity.SplitType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SplitCalculatorTest {

    private static List<SplitInput> people(Long... ids) {
        return java.util.Arrays.stream(ids).map(id -> new SplitInput(id, null)).toList();
    }

    private static BigDecimal sum(Map<Long, BigDecimal> m) {
        return m.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Test
    void equalSplit_evenlyDivisible() {
        var r = SplitCalculator.calculate(SplitType.EQUAL, new BigDecimal("900"), people(1L, 2L, 3L));
        r.values().forEach(v -> assertEquals(0, v.compareTo(new BigDecimal("300"))));
    }

    @Test
    void equalSplit_leftoverPaisaGoesToFirstPerson() {
        var r = SplitCalculator.calculate(SplitType.EQUAL, new BigDecimal("100"), people(1L, 2L, 3L));
        assertEquals(new BigDecimal("33.34"), r.get(1L));
        assertEquals(new BigDecimal("33.33"), r.get(2L));
        assertEquals(new BigDecimal("33.33"), r.get(3L));
        assertEquals(0, sum(r).compareTo(new BigDecimal("100")));
    }

    @Test
    void equalSplit_alwaysSumsToTotal() {
        for (int n = 1; n <= 9; n++) {
            Long[] ids = new Long[n];
            for (int i = 0; i < n; i++) ids[i] = (long) i + 1;
            var total = new BigDecimal("1000.07");
            var r = SplitCalculator.calculate(SplitType.EQUAL, total, people(ids));
            assertEquals(0, sum(r).compareTo(total), "failed for n=" + n);
        }
    }

    @Test
    void exactSplit_valid() {
        var inputs = List.of(new SplitInput(1L, new BigDecimal("400")), new SplitInput(2L, new BigDecimal("200")));
        var r = SplitCalculator.calculate(SplitType.EXACT, new BigDecimal("600"), inputs);
        assertEquals(0, r.get(1L).compareTo(new BigDecimal("400")));
        assertEquals(0, r.get(2L).compareTo(new BigDecimal("200")));
    }

    @Test
    void exactSplit_wrongTotalIsRejected() {
        var inputs = List.of(new SplitInput(1L, new BigDecimal("400")), new SplitInput(2L, new BigDecimal("100")));
        assertThrows(IllegalArgumentException.class,
                () -> SplitCalculator.calculate(SplitType.EXACT, new BigDecimal("600"), inputs));
    }

    @Test
    void percentageSplit_valid() {
        var inputs = List.of(new SplitInput(1L, new BigDecimal("60")), new SplitInput(2L, new BigDecimal("40")));
        var r = SplitCalculator.calculate(SplitType.PERCENTAGE, new BigDecimal("1000"), inputs);
        assertEquals(0, r.get(1L).compareTo(new BigDecimal("600")));
        assertEquals(0, r.get(2L).compareTo(new BigDecimal("400")));
    }

    @Test
    void percentageSplit_roundingDifferenceStillSumsToTotal() {
        var inputs = List.of(new SplitInput(1L, new BigDecimal("33.33")),
                new SplitInput(2L, new BigDecimal("33.33")),
                new SplitInput(3L, new BigDecimal("33.34")));
        var total = new BigDecimal("100.01");
        var r = SplitCalculator.calculate(SplitType.PERCENTAGE, total, inputs);
        assertEquals(0, sum(r).compareTo(total));
    }

    @Test
    void percentageSplit_notHundredIsRejected() {
        var inputs = List.of(new SplitInput(1L, new BigDecimal("60")), new SplitInput(2L, new BigDecimal("30")));
        assertThrows(IllegalArgumentException.class,
                () -> SplitCalculator.calculate(SplitType.PERCENTAGE, new BigDecimal("1000"), inputs));
    }

    @Test
    void duplicateUserIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> SplitCalculator.calculate(SplitType.EQUAL, new BigDecimal("100"), people(1L, 1L)));
    }

    @Test
    void zeroAmountIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> SplitCalculator.calculate(SplitType.EQUAL, BigDecimal.ZERO, people(1L, 2L)));
    }
}