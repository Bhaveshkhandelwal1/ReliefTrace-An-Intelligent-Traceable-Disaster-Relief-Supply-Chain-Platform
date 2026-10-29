package com.disasterreliefnet.transport.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request DTO for assigning a driver and vehicle to a shipment.
 *
 * @param driverId  Driver to assign
 * @param vehicleId Vehicle to assign
 */
public record AssignDriverRequest(
        @NotNull UUID driverId,
        @NotNull UUID vehicleId
) {}
