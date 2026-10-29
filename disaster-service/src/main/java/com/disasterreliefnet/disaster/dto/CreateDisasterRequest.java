package com.disasterreliefnet.disaster.dto;

import com.disasterreliefnet.common.enums.DisasterType;
import com.disasterreliefnet.common.enums.SeverityLevel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * Request payload to declare a new disaster incident.
 */
public record CreateDisasterRequest(
    @NotBlank @Size(max = 200) String name,
    @NotNull DisasterType type,
    @NotNull SeverityLevel severityLevel,
    @Size(max = 2000) String description,
    @NotNull @Valid GeoAreaDto affectedArea,
    @NotNull Instant occurredAt
) {}
