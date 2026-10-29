package com.disasterreliefnet.analytics.dto;

/**
 * High-level KPIs for victim requests through their triage and fulfillment pipeline.
 */
public record RequestSummaryResponse(
    long totalRequests,
    long pendingCount,
    long verifiedCount,
    long allocatedCount,
    long deliveredCount,
    long rejectedCount,
    PeriodDto period
) {}
