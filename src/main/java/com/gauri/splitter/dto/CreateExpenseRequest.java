package com.gauri.splitter.dto;

import com.gauri.splitter.entity.SplitType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreateExpenseRequest(
        @NotBlank String description,
        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal amount,
        Long paidByUserId,          // optional: defaults to the logged-in user
        @NotNull SplitType splitType,
        String category,            // optional
        LocalDate expenseDate,      // optional: defaults to today
        @Valid List<SplitInput> splits  // optional for EQUAL (defaults to all members)
) {}