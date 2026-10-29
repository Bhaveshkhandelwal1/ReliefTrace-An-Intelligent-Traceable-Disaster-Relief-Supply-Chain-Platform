package com.disasterreliefnet.donor.dto;

import com.disasterreliefnet.common.enums.DonorStatus;
import com.disasterreliefnet.common.enums.DonorType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DonorResponse(
    UUID donorId,
    String firstName,
    String lastName,
    String email,
    String phone,
    DonorType donorType,
    AddressDto address,
    String taxId,
    DonorStatus status,
    BigDecimal totalDonatedAmount,
    long impactDeliveriesCount,
    Instant createdAt,
    Instant updatedAt
) {}
