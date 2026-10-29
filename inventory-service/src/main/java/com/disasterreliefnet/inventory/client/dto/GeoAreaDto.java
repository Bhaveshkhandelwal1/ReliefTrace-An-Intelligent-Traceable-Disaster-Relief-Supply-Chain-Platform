package com.disasterreliefnet.inventory.client.dto;

/**
 * Geographic area representation for disaster proximity context.
 */
public record GeoAreaDto(
    Double latitude,
    Double longitude,
    Double radiusKm,
    String regionName,
    String country
) {}
