package com.disasterreliefnet.transport.dto;

import java.time.Instant;

/**
 * A single entry in a shipment's tracking history.
 *
 * @param eventType   Normalised event type string (e.g. {@code CREATED}, {@code ASSIGNED})
 * @param occurredAt  When the event occurred
 * @param description Human-readable event description
 */
public record TrackingEventDto(
        String eventType,
        Instant occurredAt,
        String description
) {}
