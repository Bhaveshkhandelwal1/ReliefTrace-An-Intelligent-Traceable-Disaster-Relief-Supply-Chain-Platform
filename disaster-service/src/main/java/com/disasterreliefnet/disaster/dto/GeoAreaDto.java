package com.disasterreliefnet.disaster.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Geospatial area representation for disaster coverage.
 */
public record GeoAreaDto(
    @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
    @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
    @DecimalMin("0.0") Double radiusKm,
    @Size(max = 200) String regionName,
    @Size(min = 2, max = 2) String country
) {}
