package com.disasterreliefnet.common.event.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

/**
 * Event payload emitted when a transport shipment is cancelled (Topic: shipment.cancelled).
 */
public record ShipmentCancelledPayload(
    @NotNull UUID shipmentId,
    @NotNull UUID allocationId,
    @NotNull UUID reservationId,
    @NotNull Instant cancelledAt,
    @NotBlank String reason
) {}
