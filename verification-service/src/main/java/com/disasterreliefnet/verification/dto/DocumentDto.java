package com.disasterreliefnet.verification.dto;

import com.disasterreliefnet.common.enums.DocumentType;

import java.time.Instant;
import java.util.UUID;

public record DocumentDto(
    UUID documentId,
    DocumentType documentType,
    String fileUrl,
    Instant uploadedAt
) {}
