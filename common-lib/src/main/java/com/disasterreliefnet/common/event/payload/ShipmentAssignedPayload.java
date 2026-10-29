package com.disasterreliefnet.common.event.payload;

import com.disasterreliefnet.common.dto.DeliveryAddressDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

/**
 * Event payload emitted when a shipment is assigned to a driver (Topic: shipment.assigned).
 */
public record ShipmentAssignedPayload(
    @NotNull UUID shipmentId,
    @NotNull UUID allocationId,
    @NotNull UUID reservationId,
    @NotNull UUID driverId,
    @NotNull UUID vehicleId,
    @NotNull UUID receiverId,
    @NotBlank String driverName,
    @NotBlank String vehiclePlate,
    Instant scheduledPickupAt,
    @NotNull DeliveryAddressDto deliveryAddress
) {}
