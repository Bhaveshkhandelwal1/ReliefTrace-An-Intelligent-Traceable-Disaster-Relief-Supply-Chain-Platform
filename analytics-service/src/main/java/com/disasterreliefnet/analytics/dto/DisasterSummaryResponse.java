package com.disasterreliefnet.analytics.dto;

import com.disasterreliefnet.common.enums.DisasterStatus;
import com.disasterreliefnet.common.enums.DisasterType;
import com.disasterreliefnet.common.enums.SeverityLevel;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * High-level disaster incident summary and operational stats.
 */
public record DisasterSummaryResponse(
    UUID disasterId,
    String disasterName,
    DisasterType type,
    SeverityLevel severityLevel,
    long totalRequestsCreated,
    long totalRequestsDelivered,
    long totalDonationsLinked,
    BigDecimal totalMonetaryDonated,
    DisasterStatus status
) {}
