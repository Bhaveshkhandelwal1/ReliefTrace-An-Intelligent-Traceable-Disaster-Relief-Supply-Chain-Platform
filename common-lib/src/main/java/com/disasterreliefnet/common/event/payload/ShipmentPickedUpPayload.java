package com.disasterreliefnet.common.event.payload;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

/**
 * Event payload emitted when supplies are picked up by the driver (Topic: shipment.picked_up).
 */
public record ShipmentPickedUpPayload(
    @NotNull UUID shipmentId,
    @NotNull UUID driverId,
    @NotNull UUID receiverId,
    @NotNull UUID allocationId,
    @NotNull Instant pickedUpAt
) {}
