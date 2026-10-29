package com.disasterreliefnet.transport.client;

import java.util.UUID;

/**
 * Client DTO representing disaster information retrieved from Disaster Service.
 */
public record DisasterClientDto(
    UUID disasterId,
    String name,
    String status,
    GeoAreaClientDto affectedArea
) {
    public record GeoAreaClientDto(
        Double latitude,
        Double longitude,
        Double radiusKm,
        String regionName,
        String country
    ) {}
}
