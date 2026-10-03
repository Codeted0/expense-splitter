package com.gauri.splitter.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

// value = exact amount (EXACT), percentage (PERCENTAGE), ignored for EQUAL
public record SplitInput(@NotNull Long userId, BigDecimal value) {}