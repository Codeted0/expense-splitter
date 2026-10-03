package com.gauri.splitter.controller;

import com.gauri.splitter.dto.GroupBalanceResponse;
import com.gauri.splitter.service.BalanceService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/groups/{groupId}/balances")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class BalanceController {

    private final BalanceService balanceService;

    @GetMapping
    public GroupBalanceResponse balances(Authentication auth, @PathVariable Long groupId) {
        return balanceService.getBalances(auth.getName(), groupId);
    }
}