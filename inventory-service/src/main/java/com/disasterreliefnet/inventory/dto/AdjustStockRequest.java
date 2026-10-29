package com.disasterreliefnet.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Request payload for manual stock increment or decrement.
 */
public record AdjustStockRequest(
    @NotNull BigDecimal adjustment,
    @NotBlank @Size(max = 500) String reason
) {}
