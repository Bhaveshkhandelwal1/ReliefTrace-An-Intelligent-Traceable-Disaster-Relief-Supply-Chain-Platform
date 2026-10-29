package com.disasterreliefnet.common.event.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

/**
 * Event payload emitted when a shipment transit fails or encounters terminal impediment (Topic: shipment.failed).
 */
public record ShipmentFailedPayload(
    @NotNull UUID shipmentId,
    @NotNull UUID allocationId,
    @NotNull UUID reservationId,
    UUID driverId,
    UUID vehicleId,
    @NotBlank String reason,
    @NotNull Instant failedAt
) {}
