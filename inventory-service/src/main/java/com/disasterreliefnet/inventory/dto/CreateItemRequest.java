package com.disasterreliefnet.inventory.dto;

import com.disasterreliefnet.common.enums.AidCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Request payload for registering a new inventory item.
 */
public record CreateItemRequest(
    @NotNull UUID warehouseId,
    @NotBlank @Size(max = 200) String name,
    @NotNull AidCategory category,
    @NotBlank @Size(max = 20) String unit,
    @DecimalMin("0") BigDecimal quantityOnHand,
    Instant expiresAt
) {}
