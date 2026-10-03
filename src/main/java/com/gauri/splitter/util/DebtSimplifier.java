package com.gauri.splitter.util;

import java.math.BigDecimal;
import java.util.*;

public final class DebtSimplifier {

    public record Transfer(Long from, Long to, BigDecimal amount) {}

    private static final class Node {
        final Long id;
        BigDecimal amount; // always positive: how much is owed to / by this person
        Node(Long id, BigDecimal amount) { this.id = id; this.amount = amount; }
    }

    private DebtSimplifier() {}

    /** net: userId -> (paid - owed). Positive = is owed money, negative = owes money. */
    public static List<Transfer> simplify(Map<Long, BigDecimal> net) {
        BigDecimal total = net.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.signum() != 0) {
            throw new IllegalStateException("Balances do not add up to zero: " + total);
        }

        // biggest amount first; ties broken by id so results are deterministic
        Comparator<Node> biggestFirst = Comparator.comparing((Node n) -> n.amount).reversed()
                .thenComparing(n -> n.id);
        PriorityQueue<Node> creditors = new PriorityQueue<>(biggestFirst);
        PriorityQueue<Node> debtors = new PriorityQueue<>(biggestFirst);

        net.forEach((id, bal) -> {
            if (bal.signum() > 0) creditors.add(new Node(id, bal));
            else if (bal.signum() < 0) debtors.add(new Node(id, bal.negate()));
        });

        List<Transfer> result = new ArrayList<>();
        while (!creditors.isEmpty() && !debtors.isEmpty()) {
            Node cr = creditors.poll();
            Node db = debtors.poll();
            BigDecimal pay = cr.amount.min(db.amount);
            result.add(new Transfer(db.id, cr.id, pay));

            cr.amount = cr.amount.subtract(pay);
            db.amount = db.amount.subtract(pay);
            if (cr.amount.signum() > 0) creditors.add(cr);
            if (db.amount.signum() > 0) debtors.add(db);
        }
        return result;
    }
}