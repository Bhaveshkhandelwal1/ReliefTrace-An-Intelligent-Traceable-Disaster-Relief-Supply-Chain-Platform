package com.disasterreliefnet.transport.dto;

import com.disasterreliefnet.common.dto.DeliveryAddressDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

/**
 * Request DTO for manually creating a shipment.
 *
 * @param allocationId      Matching allocation that triggered this shipment
 * @param reservationId     Inventory reservation confirmed for this allocation
 * @param pickupAddress     Warehouse address for goods collection
 * @param deliveryAddress   Final beneficiary delivery address
 * @param receiverId        Intended beneficiary
 * @param scheduledPickupAt Expected goods-handover time at the warehouse
 */
public record CreateShipmentRequest(
        @NotNull UUID allocationId,
        @NotNull UUID reservationId,
        @NotNull @Valid DeliveryAddressDto pickupAddress,
        @NotNull @Valid DeliveryAddressDto deliveryAddress,
        @NotNull UUID receiverId,
        @NotNull Instant scheduledPickupAt
) {}
