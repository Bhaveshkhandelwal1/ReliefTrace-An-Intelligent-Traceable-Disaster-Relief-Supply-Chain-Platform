package com.disasterreliefnet.common.event.payload;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

/**
 * Event payload emitted when a shipment is successfully delivered to the recipient (Topic: shipment.delivered).
 */
public record ShipmentDeliveredPayload(
    @NotNull UUID shipmentId,
    @NotNull UUID allocationId,
    @NotNull UUID reservationId,
    @NotNull UUID receiverId,
    @NotNull UUID driverId,
    @NotNull Instant deliveredAt,
    String proofOfDeliveryUrl
) {}
