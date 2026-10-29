package com.disasterreliefnet.common.event.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Event payload emitted when warehouse stock levels fall below the minimum threshold (Topic: stock.low).
 */
public record StockLowPayload(
    @NotNull UUID itemId,
    @NotNull UUID warehouseId,
    @NotBlank String itemName,
    @NotBlank String category,
    @NotNull BigDecimal currentQuantityAvailable,
    @NotNull BigDecimal threshold,
    @NotBlank String unit
) {}
