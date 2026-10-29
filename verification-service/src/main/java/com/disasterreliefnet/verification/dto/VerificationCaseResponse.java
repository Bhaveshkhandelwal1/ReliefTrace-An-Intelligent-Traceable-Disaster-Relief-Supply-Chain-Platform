package com.disasterreliefnet.verification.dto;

import com.disasterreliefnet.common.enums.CaseStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record VerificationCaseResponse(
    UUID caseId,
    UUID requestId,
    UUID receiverId,
    CaseStatus status,
    List<DocumentDto> documents,
    String reviewedBy,
    String reviewNotes,
    Instant openedAt,
    Instant resolvedAt,
    Instant createdAt,
    Instant updatedAt
) {}
