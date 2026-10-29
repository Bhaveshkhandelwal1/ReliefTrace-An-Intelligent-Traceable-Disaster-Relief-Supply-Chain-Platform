package com.disasterreliefnet.common.event.payload;

import com.disasterreliefnet.common.enums.FailureReason;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Event payload emitted when warehouse stock reservation fails (Topic: stock.reservation.failed).
 */
public record StockReservationFailedPayload(
    @NotNull UUID allocationId,
    @NotNull UUID requestId,
    @NotNull UUID itemId,
    @NotNull BigDecimal requestedQty,
    @NotNull BigDecimal availableQty,
    @NotNull FailureReason failureReason
) {}
