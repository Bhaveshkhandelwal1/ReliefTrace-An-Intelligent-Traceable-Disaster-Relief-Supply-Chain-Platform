package com.disasterreliefnet.common.event.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Event payload emitted when an allocation is rejected (Topic: allocation.rejected).
 */
public record AllocationRejectedPayload(
    @NotNull UUID allocationId,
    @NotNull UUID requestId,
    @NotNull UUID receiverId,
    @NotBlank String reason,
    @NotBlank String rejectedBy
) {}
