package com.gauri.splitter.controller;

import com.gauri.splitter.dto.*;
import com.gauri.splitter.service.ExpenseService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups/{groupId}/expenses")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseResponse create(Authentication auth, @PathVariable Long groupId,
                                  @Valid @RequestBody CreateExpenseRequest req) {
        return expenseService.create(auth.getName(), groupId, req);
    }

    @GetMapping
    public List<ExpenseResponse> list(Authentication auth, @PathVariable Long groupId) {
        return expenseService.list(auth.getName(), groupId);
    }

    @GetMapping("/{expenseId}")
    public ExpenseResponse get(Authentication auth, @PathVariable Long groupId, @PathVariable Long expenseId) {
        return expenseService.get(auth.getName(), groupId, expenseId);
    }

    @DeleteMapping("/{expenseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication auth, @PathVariable Long groupId, @PathVariable Long expenseId) {
        expenseService.delete(auth.getName(), groupId, expenseId);
    }
}