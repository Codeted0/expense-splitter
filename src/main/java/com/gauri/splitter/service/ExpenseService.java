package com.gauri.splitter.service;

import com.gauri.splitter.dto.*;
import com.gauri.splitter.entity.*;
import com.gauri.splitter.repository.*;
import com.gauri.splitter.util.SplitCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseGroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;

    @Transactional
    public ExpenseResponse create(String email, Long groupId, CreateExpenseRequest req) {
        ExpenseGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
        Map<Long, GroupMember> members = membersOf(groupId);
        GroupMember requester = requireMember(members, email);

        // Who paid? Must be in the group.
        Long payerId = req.paidByUserId() != null ? req.paidByUserId() : requester.getUser().getId();
        GroupMember payer = members.get(payerId);
        if (payer == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payer is not a member of this group");
        }

        // Who shares it? EQUAL with no list means everyone in the group.
        List<SplitInput> inputs = req.splits();
        if ((inputs == null || inputs.isEmpty()) && req.splitType() == SplitType.EQUAL) {
            inputs = members.keySet().stream().map(id -> new SplitInput(id, null)).toList();
        }
        if (inputs != null) {
            for (SplitInput in : inputs) {
                if (!members.containsKey(in.userId())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "User " + in.userId() + " is not a member of this group");
                }
            }
        }

        Map<Long, BigDecimal> shares;
        try {
            shares = SplitCalculator.calculate(req.splitType(), req.amount(), inputs);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        Expense expense = Expense.builder()
                .group(group)
                .paidBy(payer.getUser())
                .amount(req.amount().setScale(2))
                .description(req.description())
                .category(req.category())
                .splitType(req.splitType())
                .expenseDate(req.expenseDate() != null ? req.expenseDate() : LocalDate.now())
                .build();

        shares.forEach((userId, share) -> expense.getSplits().add(
                ExpenseSplit.builder()
                        .expense(expense)
                        .user(members.get(userId).getUser())
                        .shareAmount(share)
                        .build()));

        return toResponse(expenseRepository.save(expense));
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> list(String email, Long groupId) {
        requireMember(membersOf(groupId), email);
        return expenseRepository.findByGroupIdOrderByExpenseDateDescIdDesc(groupId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ExpenseResponse get(String email, Long groupId, Long expenseId) {
        requireMember(membersOf(groupId), email);
        return toResponse(findInGroup(groupId, expenseId));
    }

    @Transactional
    public void delete(String email, Long groupId, Long expenseId) {
        GroupMember requester = requireMember(membersOf(groupId), email);
        Expense expense = findInGroup(groupId, expenseId);
        boolean isPayer = expense.getPaidBy().getId().equals(requester.getUser().getId());
        if (!isPayer && requester.getRole() != GroupRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the payer or a group admin can delete this expense");
        }
        expenseRepository.delete(expense);
    }

    // ---- helpers ----

    private Map<Long, GroupMember> membersOf(Long groupId) {
        return memberRepository.findByGroupIdWithUser(groupId).stream()
                .collect(Collectors.toMap(m -> m.getUser().getId(), Function.identity(),
                        (a, b) -> a, LinkedHashMap::new));
    }

    private GroupMember requireMember(Map<Long, GroupMember> members, String email) {
        return members.values().stream()
                .filter(m -> m.getUser().getEmail().equals(email))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "You are not a member of this group"));
    }

    private Expense findInGroup(Long groupId, Long expenseId) {
        Expense e = expenseRepository.findWithDetailsById(expenseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Expense not found"));
        if (!e.getGroup().getId().equals(groupId)) { // stops reading expenses of other groups
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Expense not found");
        }
        return e;
    }

    private ExpenseResponse toResponse(Expense e) {
        List<SplitResponse> splits = e.getSplits().stream()
                .map(s -> new SplitResponse(s.getUser().getId(), s.getUser().getName(), s.getShareAmount()))
                .toList();
        return new ExpenseResponse(e.getId(), e.getDescription(), e.getAmount(), e.getCategory(),
                e.getSplitType(), e.getExpenseDate(), e.getPaidBy().getId(), e.getPaidBy().getName(), splits);
    }
}