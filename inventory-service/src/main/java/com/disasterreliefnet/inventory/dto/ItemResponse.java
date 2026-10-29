package com.disasterreliefnet.inventory.dto;

import com.disasterreliefnet.common.enums.AidCategory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response payload representing an inventory item and its real-time stock levels.
 */
public record ItemResponse(
    UUID itemId,
    UUID warehouseId,
    String name,
    AidCategory category,
    String unit,
    BigDecimal quantityOnHand,
    BigDecimal quantityReserved,
    BigDecimal quantityAvailable,
    Instant expiresAt,
    Instant createdAt,
    Instant updatedAt
) {}
