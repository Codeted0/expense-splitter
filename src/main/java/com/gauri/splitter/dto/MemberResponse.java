package com.gauri.splitter.dto;

import com.gauri.splitter.entity.GroupRole;

public record MemberResponse(Long userId, String name, String email, GroupRole role) {}