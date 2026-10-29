package com.disasterreliefnet.finance.dto;

import com.disasterreliefnet.common.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for a payment record.
 */
public record PaymentRecordResponse(
    UUID paymentId,
    UUID donationId,
    UUID donorId,
    BigDecimal amount,
    String currency,
    PaymentStatus status,
    String gatewayReference,
    Instant processedAt,
    Instant createdAt,
    Instant updatedAt
) {}
