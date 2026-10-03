package com.gauri.splitter.controller;

import com.gauri.splitter.dto.*;
import com.gauri.splitter.service.SettlementService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups/{groupId}/settlements")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class SettlementController {

    private final SettlementService settlementService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SettlementResponse create(Authentication auth, @PathVariable Long groupId,
                                     @Valid @RequestBody CreateSettlementRequest req) {
        return settlementService.create(auth.getName(), groupId, req);
    }

    @GetMapping
    public List<SettlementResponse> list(Authentication auth, @PathVariable Long groupId) {
        return settlementService.list(auth.getName(), groupId);
    }

    @PostMapping("/{settlementId}/confirm")
    public SettlementResponse confirm(Authentication auth, @PathVariable Long groupId,
                                      @PathVariable Long settlementId) {
        return settlementService.confirm(auth.getName(), groupId, settlementId);
    }

    @DeleteMapping("/{settlementId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(Authentication auth, @PathVariable Long groupId, @PathVariable Long settlementId) {
        settlementService.cancel(auth.getName(), groupId, settlementId);
    }
}