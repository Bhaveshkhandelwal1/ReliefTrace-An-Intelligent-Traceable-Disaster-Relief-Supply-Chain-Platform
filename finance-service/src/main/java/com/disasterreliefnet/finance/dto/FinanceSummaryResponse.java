package com.disasterreliefnet.finance.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Summary statistics DTO of financial platform balances.
 */
public record FinanceSummaryResponse(
    BigDecimal totalDonationsReceived,
    BigDecimal totalDisbursed,
    BigDecimal netBalance,
    String currency,
    Instant periodFrom,
    Instant periodTo
) {}
