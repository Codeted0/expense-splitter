package com.gauri.splitter.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CreateSettlementRequest(
        @NotNull Long toUserId,
        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal amount
) {}