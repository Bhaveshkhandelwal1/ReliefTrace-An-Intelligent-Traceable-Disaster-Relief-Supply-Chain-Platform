package com.disasterreliefnet.analytics.dto;

import java.util.UUID;

/**
 * Composite executive situational dashboard response.
 */
public record DashboardResponse(
    UUID disasterId,
    DonationSummaryResponse donationSummary,
    RequestSummaryResponse requestSummary,
    ShipmentSummaryResponse shipmentSummary,
    long activeDisasters
) {}
