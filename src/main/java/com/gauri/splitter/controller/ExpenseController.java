package com.gauri.splitter.controller;

import com.gauri.splitter.dto.*;
import com.gauri.splitter.service.ExpenseService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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
    public PageResponse<ExpenseResponse> list(
            Authentication auth, @PathVariable Long groupId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long memberId,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return expenseService.search(auth.getName(), groupId, from, to, memberId, category, page, size);
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