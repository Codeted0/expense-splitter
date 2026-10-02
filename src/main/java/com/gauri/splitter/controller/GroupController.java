package com.gauri.splitter.controller;

import com.gauri.splitter.dto.*;
import com.gauri.splitter.service.GroupService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class GroupController {

    private final GroupService groupService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GroupResponse create(Authentication auth, @Valid @RequestBody CreateGroupRequest req) {
        return groupService.createGroup(auth.getName(), req);
    }

    @GetMapping
    public List<GroupResponse> myGroups(Authentication auth) {
        return groupService.myGroups(auth.getName());
    }

    @GetMapping("/{groupId}")
    public GroupResponse get(Authentication auth, @PathVariable Long groupId) {
        return groupService.getGroup(auth.getName(), groupId);
    }

    @PostMapping("/{groupId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponse addMember(Authentication auth, @PathVariable Long groupId,
                                    @Valid @RequestBody AddMemberRequest req) {
        return groupService.addMember(auth.getName(), groupId, req);
    }

    @GetMapping("/{groupId}/members")
    public List<MemberResponse> members(Authentication auth, @PathVariable Long groupId) {
        return groupService.listMembers(auth.getName(), groupId);
    }
}