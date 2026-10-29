package com.disasterreliefnet.transport.dto;

import com.disasterreliefnet.transport.domain.VehicleType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for a vehicle record.
 *
 * @param vehicleId   Unique vehicle identifier
 * @param driverId    Owning driver identifier
 * @param plateNumber Licence plate
 * @param type        Vehicle category
 * @param capacityKg  Max payload in kg
 * @param capacityCbm Max volume in m³
 * @param active      Whether the vehicle is active
 * @param createdAt   Record creation timestamp
 * @param updatedAt   Record last-updated timestamp
 */
public record VehicleResponse(
        UUID vehicleId,
        UUID driverId,
        String plateNumber,
        VehicleType type,
        BigDecimal capacityKg,
        BigDecimal capacityCbm,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {}
