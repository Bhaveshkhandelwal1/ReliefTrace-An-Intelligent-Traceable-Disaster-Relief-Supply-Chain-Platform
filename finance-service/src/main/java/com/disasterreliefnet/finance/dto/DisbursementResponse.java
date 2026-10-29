package com.disasterreliefnet.finance.dto;

import com.disasterreliefnet.common.enums.DisbursementStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for a relief aid disbursement.
 */
public record DisbursementResponse(
    UUID disbursementId,
    UUID allocationId,
    BigDecimal amount,
    String currency,
    DisbursementStatus status,
    String authorizedBy,
    Instant paidAt,
    Instant createdAt,
    Instant updatedAt
) {}
