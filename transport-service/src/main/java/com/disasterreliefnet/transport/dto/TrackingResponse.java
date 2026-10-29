package com.disasterreliefnet.transport.dto;

import com.disasterreliefnet.transport.domain.ShipmentStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for the shipment tracking view.
 *
 * @param shipmentId    Unique shipment identifier
 * @param status        Current shipment status
 * @param driverName    Full name of assigned driver (null if not yet assigned)
 * @param vehiclePlate  Plate number of assigned vehicle (null if not yet assigned)
 * @param lastUpdatedAt Timestamp of last status update
 * @param events        Ordered list of tracking events
 */
public record TrackingResponse(
        UUID shipmentId,
        ShipmentStatus status,
        String driverName,
        String vehiclePlate,
        Instant lastUpdatedAt,
        List<TrackingEventDto> events
) {}
