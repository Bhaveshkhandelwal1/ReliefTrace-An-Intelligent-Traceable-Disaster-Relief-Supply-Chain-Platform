package com.disasterreliefnet.analytics.dto;

import java.time.Instant;

/**
 * Time window filter applied to an analytics query.
 */
public record PeriodDto(
    Instant from,
    Instant to
) {}
