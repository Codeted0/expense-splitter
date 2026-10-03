package com.gauri.splitter.util;

import com.gauri.splitter.util.DebtSimplifier.Transfer;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class DebtSimplifierTest {

    private static BigDecimal bd(String s) { return new BigDecimal(s); }

    @Test
    void projectExample_twoTransfers() {
        Map<Long, BigDecimal> net = new LinkedHashMap<>();
        net.put(1L, bd("400.00"));   // A is owed 400
        net.put(2L, bd("100.00"));   // B is owed 100
        net.put(3L, bd("-500.00"));  // C owes 500

        List<Transfer> result = DebtSimplifier.simplify(net);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(t -> t.from().equals(3L)));
        assertEquals(0, find(result, 1L).amount().compareTo(bd("400")));
        assertEquals(0, find(result, 2L).amount().compareTo(bd("100")));
    }

    private static Transfer find(List<Transfer> list, Long to) {
        return list.stream().filter(t -> t.to().equals(to)).findFirst().orElseThrow();
    }

    @Test
    void everyoneSettled_noTransfers() {
        Map<Long, BigDecimal> net = Map.of(1L, BigDecimal.ZERO, 2L, BigDecimal.ZERO);
        assertTrue(DebtSimplifier.simplify(net).isEmpty());
    }

    @Test
    void unbalancedInput_isRejected() {
        Map<Long, BigDecimal> net = Map.of(1L, bd("10.00"), 2L, bd("-5.00"));
        assertThrows(IllegalStateException.class, () -> DebtSimplifier.simplify(net));
    }

    @Test
    void transfersClearEveryBalance_andNeverExceedNMinusOne() {
        Map<Long, BigDecimal> net = new LinkedHashMap<>();
        net.put(1L, bd("250.50"));
        net.put(2L, bd("-100.25"));
        net.put(3L, bd("-50.25"));
        net.put(4L, bd("80.00"));
        net.put(5L, bd("-180.00"));
        net.put(6L, bd("0.00"));

        List<Transfer> result = DebtSimplifier.simplify(net);

        // apply the transfers and check everybody ends at zero
        Map<Long, BigDecimal> after = new HashMap<>(net);
        for (Transfer t : result) {
            after.merge(t.from(), t.amount(), BigDecimal::add);              // payer owes less
            after.merge(t.to(), t.amount().negate(), BigDecimal::add);       // receiver is owed less
        }
        after.values().forEach(v -> assertEquals(0, v.signum()));
        assertTrue(result.size() <= 4); // 5 people with a non-zero balance -> at most 4 transfers
    }
}