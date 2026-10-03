package com.gauri.splitter.dto;

import com.gauri.splitter.entity.SplitType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ExpenseResponse(Long id, String description, BigDecimal amount, String category,
                              SplitType splitType, LocalDate expenseDate,
                              Long paidById, String paidByName, List<SplitResponse> splits) {}