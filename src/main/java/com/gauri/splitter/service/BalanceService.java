package com.gauri.splitter.service;

import com.gauri.splitter.dto.*;
import com.gauri.splitter.entity.*;
import com.gauri.splitter.repository.*;
import com.gauri.splitter.util.DebtSimplifier;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.gauri.splitter.entity.SettlementStatus;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
public class BalanceService {

    private final ExpenseRepository expenseRepository;
    private final GroupMemberRepository memberRepository;
    private final SettlementRepository settlementRepository;

    @Transactional(readOnly = true)
    public GroupBalanceResponse getBalances(String email, Long groupId) {
        List<GroupMember> members = memberRepository.findByGroupIdWithUser(groupId);
        boolean isMember = members.stream().anyMatch(m -> m.getUser().getEmail().equals(email));
        if (!isMember) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this group");
        }

        Map<Long, BigDecimal> net = new LinkedHashMap<>();
        Map<Long, String> names = new HashMap<>();
        for (GroupMember m : members) {
            net.put(m.getUser().getId(), BigDecimal.ZERO);
            names.put(m.getUser().getId(), m.getUser().getName());
        }

        // Step 1: net balance = what you paid - what you owe
        for (Expense e : expenseRepository.findByGroupIdOrderByExpenseDateDescIdDesc(groupId)) {
            net.merge(e.getPaidBy().getId(), e.getAmount(), BigDecimal::add);
            for (ExpenseSplit s : e.getSplits()) {
                net.merge(s.getUser().getId(), s.getShareAmount().negate(), BigDecimal::add);
            }
        }

        // Step 2: simplify into the fewest payments
        List<TransferResponse> settlements = DebtSimplifier.simplify(net).stream()
                .map(t -> new TransferResponse(t.from(), names.get(t.from()),
                        t.to(), names.get(t.to()), t.amount()))
                .toList();

        List<BalanceResponse> balances = net.entrySet().stream()
                .map(en -> new BalanceResponse(en.getKey(), names.get(en.getKey()), en.getValue()))
                .toList();

        // Confirmed payments: the payer owes less, the receiver is owed less
        for (Settlement st : settlementRepository.findByGroupIdWithUsers(groupId)) {
            if (st.getStatus() == SettlementStatus.PAID) {
                net.merge(st.getFromUser().getId(), st.getAmount(), BigDecimal::add);
                net.merge(st.getToUser().getId(), st.getAmount().negate(), BigDecimal::add);
            }
        }

        return new GroupBalanceResponse(balances, settlements);
    }
}