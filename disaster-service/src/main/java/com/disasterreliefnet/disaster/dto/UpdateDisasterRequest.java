package com.disasterreliefnet.disaster.dto;

import com.disasterreliefnet.common.enums.SeverityLevel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Request payload to update an existing disaster's details and affected population.
 */
public record UpdateDisasterRequest(
    SeverityLevel severityLevel,
    @Size(max = 2000) String description,
    @Valid GeoAreaDto affectedArea,
    @Min(0) Long estimatedAffectedPeople
) {}
