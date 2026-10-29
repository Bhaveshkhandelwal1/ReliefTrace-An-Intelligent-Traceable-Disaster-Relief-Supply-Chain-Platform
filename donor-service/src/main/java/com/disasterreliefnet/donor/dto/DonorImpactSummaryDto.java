package com.disasterreliefnet.donor.dto;

import com.disasterreliefnet.common.enums.DonorType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Historical impact summary for a donor.
 */
public record DonorImpactSummaryDto(
    UUID donorId,
    String donorName,
    DonorType donorType,
    long totalDonationsCount,
    BigDecimal totalMonetaryAmount,
    long impactDeliveriesCount,
    Instant memberSince
) {}
