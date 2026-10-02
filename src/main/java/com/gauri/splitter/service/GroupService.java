package com.gauri.splitter.service;

import com.gauri.splitter.dto.*;
import com.gauri.splitter.entity.*;
import com.gauri.splitter.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final ExpenseGroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;
    private final UserRepository userRepository;

    @Transactional
    public GroupResponse createGroup(String email, CreateGroupRequest req) {
        User creator = getUser(email);
        ExpenseGroup group = groupRepository.save(ExpenseGroup.builder()
                .name(req.name())
                .createdBy(creator)
                .build());
        memberRepository.save(GroupMember.builder()
                .group(group).user(creator).role(GroupRole.ADMIN).build());
        return new GroupResponse(group.getId(), group.getName(), creator.getName(), 1);
    }

    @Transactional(readOnly = true)
    public List<GroupResponse> myGroups(String email) {
        User user = getUser(email);
        return memberRepository.findByUserIdWithGroup(user.getId()).stream()
                .map(m -> toResponse(m.getGroup()))
                .toList();
    }

    @Transactional(readOnly = true)
    public GroupResponse getGroup(String email, Long groupId) {
        ExpenseGroup group = getGroupOrThrow(groupId);
        requireMember(groupId, getUser(email));
        return toResponse(group);
    }

    @Transactional
    public MemberResponse addMember(String email, Long groupId, AddMemberRequest req) {
        ExpenseGroup group = getGroupOrThrow(groupId);
        GroupMember requester = requireMember(groupId, getUser(email));
        if (requester.getRole() != GroupRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only group admins can add members");
        }

        User newUser = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No registered user with that email"));
        if (memberRepository.existsByGroupIdAndUserId(groupId, newUser.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User is already in this group");
        }

        memberRepository.save(GroupMember.builder()
                .group(group).user(newUser).role(GroupRole.MEMBER).build());
        return new MemberResponse(newUser.getId(), newUser.getName(), newUser.getEmail(), GroupRole.MEMBER);
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> listMembers(String email, Long groupId) {
        getGroupOrThrow(groupId);
        requireMember(groupId, getUser(email));
        return memberRepository.findByGroupIdWithUser(groupId).stream()
                .map(m -> new MemberResponse(m.getUser().getId(), m.getUser().getName(),
                        m.getUser().getEmail(), m.getRole()))
                .toList();
    }

    // ---- helpers ----

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private ExpenseGroup getGroupOrThrow(Long id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
    }

    // The key security check: the caller must belong to the group
    private GroupMember requireMember(Long groupId, User user) {
        return memberRepository.findByGroupIdAndUserId(groupId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "You are not a member of this group"));
    }

    private GroupResponse toResponse(ExpenseGroup g) {
        return new GroupResponse(g.getId(), g.getName(), g.getCreatedBy().getName(),
                memberRepository.countByGroupId(g.getId()));
    }
}