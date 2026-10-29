package com.disasterreliefnet.transport.dto;

import com.disasterreliefnet.transport.domain.VehicleType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request DTO for registering a new vehicle.
 *
 * @param driverId    Owner driver
 * @param plateNumber Licence plate
 * @param type        Vehicle category
 * @param capacityKg  Max load in kilograms
 * @param capacityCbm Max volume in cubic metres
 */
public record CreateVehicleRequest(
        @NotNull UUID driverId,
        @NotBlank @Size(max = 20) String plateNumber,
        @NotNull VehicleType type,
        @DecimalMin("0") BigDecimal capacityKg,
        @DecimalMin("0") BigDecimal capacityCbm
) {}
