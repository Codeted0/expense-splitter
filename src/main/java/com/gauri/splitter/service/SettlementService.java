package com.gauri.splitter.service;

import com.gauri.splitter.dto.*;
import com.gauri.splitter.entity.*;
import com.gauri.splitter.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final ExpenseGroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;

    @Transactional
    public SettlementResponse create(String email, Long groupId, CreateSettlementRequest req) {
        ExpenseGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
        List<GroupMember> members = memberRepository.findByGroupIdWithUser(groupId);
        User payer = requireMember(members, email).getUser();

        User receiver = members.stream().map(GroupMember::getUser)
                .filter(u -> u.getId().equals(req.toUserId())).findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Receiver is not a member of this group"));
        if (receiver.getId().equals(payer.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot pay yourself");
        }

        Settlement saved = settlementRepository.save(Settlement.builder()
                .group(group).fromUser(payer).toUser(receiver)
                .amount(req.amount().setScale(2))
                .status(SettlementStatus.PENDING)
                .build());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<SettlementResponse> list(String email, Long groupId) {
        requireMember(memberRepository.findByGroupIdWithUser(groupId), email);
        return settlementRepository.findByGroupIdWithUsers(groupId).stream()
                .map(this::toResponse).toList();
    }

    // Only the RECEIVER can confirm: they are the one who knows the money arrived
    @Transactional
    public SettlementResponse confirm(String email, Long groupId, Long settlementId) {
        requireMember(memberRepository.findByGroupIdWithUser(groupId), email);
        Settlement s = findInGroup(groupId, settlementId);
        if (!s.getToUser().getEmail().equals(email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the receiver can confirm a payment");
        }
        if (s.getStatus() != SettlementStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Payment is already confirmed");
        }
        s.setStatus(SettlementStatus.PAID);
        s.setPaidAt(LocalDateTime.now());
        return toResponse(s);
    }

    // The payer can withdraw a payment that has not been confirmed yet
    @Transactional
    public void cancel(String email, Long groupId, Long settlementId) {
        requireMember(memberRepository.findByGroupIdWithUser(groupId), email);
        Settlement s = findInGroup(groupId, settlementId);
        if (!s.getFromUser().getEmail().equals(email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the payer can cancel this payment");
        }
        if (s.getStatus() != SettlementStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A confirmed payment cannot be cancelled");
        }
        settlementRepository.delete(s);
    }

    // ---- helpers ----

    private GroupMember requireMember(List<GroupMember> members, String email) {
        return members.stream().filter(m -> m.getUser().getEmail().equals(email)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "You are not a member of this group"));
    }

    private Settlement findInGroup(Long groupId, Long id) {
        Settlement s = settlementRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Settlement not found"));
        if (!s.getGroup().getId().equals(groupId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Settlement not found");
        }
        return s;
    }

    private SettlementResponse toResponse(Settlement s) {
        return new SettlementResponse(s.getId(),
                s.getFromUser().getId(), s.getFromUser().getName(),
                s.getToUser().getId(), s.getToUser().getName(),
                s.getAmount(), s.getStatus(), s.getCreatedAt(), s.getPaidAt());
    }
}