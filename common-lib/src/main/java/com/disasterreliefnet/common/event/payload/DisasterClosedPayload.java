package com.disasterreliefnet.common.event.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

/**
 * Event payload emitted when a disaster operation is closed (Topic: disaster.closed).
 */
public record DisasterClosedPayload(
    @NotNull UUID disasterId,
    @NotBlank String closureReason,
    @NotBlank String closedBy,
    @NotNull Instant closedAt
) {}
