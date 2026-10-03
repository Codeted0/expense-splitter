package com.gauri.splitter.dto;

import java.math.BigDecimal;

public record SplitResponse(Long userId, String name, BigDecimal shareAmount) {}