package com.disasterreliefnet.disaster.dto;

import com.disasterreliefnet.common.enums.DisasterStatus;
import com.disasterreliefnet.common.enums.DisasterType;
import com.disasterreliefnet.common.enums.SeverityLevel;

import java.time.Instant;
import java.util.UUID;

/**
 * Detailed response for a disaster incident.
 */
public record DisasterResponse(
    UUID disasterId,
    String name,
    DisasterType type,
    SeverityLevel severityLevel,
    String description,
    GeoAreaDto affectedArea,
    long estimatedAffectedPeople,
    DisasterStatus status,
    Instant occurredAt,
    Instant closedAt,
    Instant createdAt,
    Instant updatedAt
) {}
