package com.disasterreliefnet.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Geographic area affected by a declared disaster incident.
 */
public record AffectedAreaDto(
    @NotNull Double latitude,
    @NotNull Double longitude,
    @NotNull Double radiusKm,
    @NotBlank String regionName,
    @NotBlank @Size(min = 2, max = 2) String country
) {}
