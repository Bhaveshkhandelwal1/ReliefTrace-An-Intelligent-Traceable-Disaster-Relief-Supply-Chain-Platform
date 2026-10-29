package com.disasterreliefnet.common.event.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Event payload emitted when a financial donation payment completes (Topic: payment.completed).
 */
public record PaymentCompletedPayload(
    @NotNull UUID paymentId,
    @NotNull UUID donationId,
    @NotNull UUID donorId,
    @NotNull BigDecimal amount,
    @NotBlank String currency,
    @NotBlank String gatewayReference,
    @NotNull Instant processedAt
) {}
