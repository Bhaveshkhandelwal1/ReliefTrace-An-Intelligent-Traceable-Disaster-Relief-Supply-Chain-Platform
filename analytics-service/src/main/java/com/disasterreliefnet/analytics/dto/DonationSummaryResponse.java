package com.disasterreliefnet.analytics.dto;

import java.math.BigDecimal;

/**
 * High-level KPIs for financial pledges and in-kind relief aid collected.
 */
public record DonationSummaryResponse(
    long totalDonations,
    BigDecimal totalMonetaryAmount,
    BigDecimal totalInKindKg,
    String currency,
    PeriodDto period
) {}
