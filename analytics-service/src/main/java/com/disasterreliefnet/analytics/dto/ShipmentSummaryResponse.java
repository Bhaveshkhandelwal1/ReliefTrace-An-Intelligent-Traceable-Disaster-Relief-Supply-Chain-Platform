package com.disasterreliefnet.analytics.dto;

/**
 * High-level KPIs for transport network deliveries, active runs, disputes, and speed.
 */
public record ShipmentSummaryResponse(
    long totalShipments,
    long deliveredCount,
    long inTransitCount,
    long disputedCount,
    double avgDeliveryHours,
    PeriodDto period
) {}
