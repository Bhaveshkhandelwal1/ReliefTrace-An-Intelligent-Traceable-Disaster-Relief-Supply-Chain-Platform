package com.disasterreliefnet.transport.dto;

import com.disasterreliefnet.common.dto.DeliveryAddressDto;
import com.disasterreliefnet.transport.domain.ShipmentStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for a shipment.
 *
 * @param shipmentId          Unique shipment identifier
 * @param allocationId        Matching allocation
 * @param reservationId       Inventory reservation
 * @param driverId            Assigned driver (null until assigned)
 * @param vehicleId           Assigned vehicle (null until assigned)
 * @param receiverId          Intended beneficiary
 * @param pickupAddress       Warehouse pick-up address
 * @param deliveryAddress     Beneficiary delivery address
 * @param scheduledPickupAt   Target pick-up time
 * @param pickedUpAt          Actual pick-up time
 * @param deliveredAt         Actual delivery time
 * @param status              Current shipment status
 * @param proofOfDeliveryUrl  URL of proof of delivery document
 * @param createdAt           Record creation timestamp
 * @param updatedAt           Record last-updated timestamp
 */
public record ShipmentResponse(
        UUID shipmentId,
        UUID allocationId,
        UUID reservationId,
        UUID driverId,
        UUID vehicleId,
        UUID receiverId,
        DeliveryAddressDto pickupAddress,
        DeliveryAddressDto deliveryAddress,
        Instant scheduledPickupAt,
        Instant pickedUpAt,
        Instant deliveredAt,
        ShipmentStatus status,
        String proofOfDeliveryUrl,
        Instant createdAt,
        Instant updatedAt
) {}
