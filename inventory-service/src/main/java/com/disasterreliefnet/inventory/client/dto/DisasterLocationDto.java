package com.disasterreliefnet.inventory.client.dto;

import java.util.UUID;

/**
 * Disaster details record retrieved from disaster-service.
 */
public record DisasterLocationDto(
    UUID disasterId,
    String name,
    String type,
    String severityLevel,
    GeoAreaDto affectedArea
) {}
