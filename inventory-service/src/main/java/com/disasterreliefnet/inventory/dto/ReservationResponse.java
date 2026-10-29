package com.disasterreliefnet.inventory.dto;

import com.disasterreliefnet.common.enums.ReservationStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response payload representing an active or completed inventory reservation.
 */
public record ReservationResponse(
    UUID reservationId,
    UUID itemId,
    UUID allocationId,
    BigDecimal quantity,
    ReservationStatus status,
    Instant createdAt,
    Instant updatedAt
) {}
