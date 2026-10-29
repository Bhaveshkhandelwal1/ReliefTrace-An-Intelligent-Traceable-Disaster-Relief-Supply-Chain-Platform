package com.disasterreliefnet.common.event.payload;

import com.disasterreliefnet.common.dto.AffectedAreaDto;
import com.disasterreliefnet.common.enums.DisasterType;
import com.disasterreliefnet.common.enums.SeverityLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

/**
 * Event payload emitted when a new disaster incident is declared (Topic: disaster.declared).
 */
public record DisasterDeclaredPayload(
    @NotNull UUID disasterId,
    @NotBlank String name,
    @NotNull DisasterType type,
    @NotNull SeverityLevel severityLevel,
    @NotNull AffectedAreaDto affectedArea,
    @NotNull Instant occurredAt
) {}
