package com.disasterreliefnet.donor.client.dto;

import com.disasterreliefnet.common.enums.VerificationStatus;

import java.util.UUID;

public record VerificationStatusDto(
    UUID entityId,
    VerificationStatus status,
    String notes
) {}
