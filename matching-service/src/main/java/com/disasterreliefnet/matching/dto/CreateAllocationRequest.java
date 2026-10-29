package com.disasterreliefnet.matching.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateAllocationRequest(
    @NotNull(message = "Request ID is required")
    UUID requestId,

    @NotNull(message = "Item ID is required")
    UUID itemId,

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.01", message = "Quantity must be at least 0.01")
    BigDecimal quantity,

    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    String notes
) {}
