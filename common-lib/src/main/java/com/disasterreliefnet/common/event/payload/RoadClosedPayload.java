package com.disasterreliefnet.common.event.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

/**
 * Event payload emitted when a road or transit corridor is closed due to disaster hazard.
 */
public record RoadClosedPayload(
    @NotNull UUID disasterId,
    @NotBlank String roadSegmentId,
    @NotBlank String roadName,
    Double latitude,
    Double longitude,
    Double radiusKm,
    @NotBlank String reason,
    @NotNull Instant occurredAt
) {}
