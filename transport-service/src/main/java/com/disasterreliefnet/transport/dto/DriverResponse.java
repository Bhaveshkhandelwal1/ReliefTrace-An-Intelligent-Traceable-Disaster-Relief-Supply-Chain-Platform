package com.disasterreliefnet.transport.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for a driver profile.
 *
 * @param driverId         Unique driver identifier
 * @param userId           Keycloak user ID
 * @param firstName        Given name
 * @param lastName         Family name
 * @param email            Contact email
 * @param phone            Contact phone
 * @param licenseNumber    Driving licence
 * @param available        Whether driver is ready to take a new shipment
 * @param activeShipmentId The shipment the driver is currently working on (null if none)
 * @param createdAt        Record creation timestamp
 * @param updatedAt        Record last-updated timestamp
 */
public record DriverResponse(
        UUID driverId,
        String userId,
        String firstName,
        String lastName,
        String email,
        String phone,
        String licenseNumber,
        boolean available,
        UUID activeShipmentId,
        Instant createdAt,
        Instant updatedAt
) {}
