package com.disasterreliefnet.common.event.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Event payload emitted when a case is verified by a coordinator (Topic: request.verified).
 */
public record RequestVerifiedPayload(
    @NotNull UUID requestId,
    @NotNull UUID receiverId,
    @NotNull UUID caseId,
    @NotBlank String outcome, // APPROVED | REJECTED
    @NotBlank String reviewedBy,
    String reviewNotes
) {}
