package com.gauri.splitter.dto;

import com.gauri.splitter.entity.SettlementStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SettlementResponse(Long id, Long fromUserId, String fromName,
                                 Long toUserId, String toName, BigDecimal amount,
                                 SettlementStatus status, LocalDateTime createdAt,
                                 LocalDateTime paidAt) {}