package com.gauri.splitter.dto;

import java.math.BigDecimal;

public record TransferResponse(Long fromUserId, String fromName,
                               Long toUserId, String toName, BigDecimal amount) {}