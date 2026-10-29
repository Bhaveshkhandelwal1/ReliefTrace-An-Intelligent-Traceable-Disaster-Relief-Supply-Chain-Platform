package com.disasterreliefnet.common.event.payload;

import com.disasterreliefnet.common.enums.SeverityLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Event payload emitted when a disaster severity or affected people estimate changes (Topic: disaster.updated).
 */
public record DisasterUpdatedPayload(
    @NotNull UUID disasterId,
    @NotNull SeverityLevel severityLevel,
    Integer estimatedAffectedPeople,
    @NotBlank String updatedBy
) {}
