package com.disasterreliefnet.common.event.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Event payload emitted when a shipment delivery is disputed (Topic: delivery.disputed).
 */
public record DeliveryDisputedPayload(
    @NotNull UUID shipmentId,
    @NotNull UUID allocationId,
    @NotNull UUID receiverId,
    UUID driverId,
    @NotBlank String reason,
    String evidenceUrl,
    @NotBlank String disputedBy // DRIVER | RECEIVER
) {}
