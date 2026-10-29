package com.disasterreliefnet.inventory.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response payload describing a warehouse facility.
 */
public record WarehouseResponse(
    UUID warehouseId,
    String name,
    AddressDto address,
    BigDecimal capacity,
    boolean active,
    Instant createdAt,
    Instant updatedAt
) {}
