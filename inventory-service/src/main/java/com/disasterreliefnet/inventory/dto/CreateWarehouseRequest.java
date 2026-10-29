package com.disasterreliefnet.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Request payload for creating a new warehouse facility.
 */
public record CreateWarehouseRequest(
    @NotBlank @Size(max = 200) String name,
    @NotNull @Valid AddressDto address,
    @DecimalMin("0") BigDecimal capacity
) {}
