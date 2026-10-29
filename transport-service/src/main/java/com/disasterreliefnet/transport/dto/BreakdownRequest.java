package com.disasterreliefnet.transport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for reporting a vehicle breakdown.
 */
public record BreakdownRequest(
    @NotBlank @Size(max = 500) String reason
) {}
