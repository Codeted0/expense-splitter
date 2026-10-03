package com.gauri.splitter.dto;

import java.math.BigDecimal;

public record BalanceResponse(Long userId, String name, BigDecimal netBalance) {}