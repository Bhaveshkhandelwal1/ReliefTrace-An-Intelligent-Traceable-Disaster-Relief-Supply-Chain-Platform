package com.disasterreliefnet.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Request payload for updating an existing warehouse.
 */
public record UpdateWarehouseRequest(
    @Size(max = 200) String name,
    @Valid AddressDto address,
    @DecimalMin("0") BigDecimal capacity
) {}
