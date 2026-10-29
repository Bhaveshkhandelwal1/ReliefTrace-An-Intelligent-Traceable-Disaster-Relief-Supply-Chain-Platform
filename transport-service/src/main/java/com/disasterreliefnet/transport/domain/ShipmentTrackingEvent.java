package com.disasterreliefnet.transport.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable tracking event appended to a shipment's audit trail.
 */
@Entity
@Table(name = "shipment_tracking_events")
@Getter
@Setter
@NoArgsConstructor
public class ShipmentTrackingEvent {

    @Id
    @Column(name = "tracking_event_id", nullable = false, updatable = false)
    private UUID trackingEventId;

    @Column(name = "shipment_id", nullable = false)
    private UUID shipmentId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (trackingEventId == null) {
            trackingEventId = UUID.randomUUID();
        }
        createdAt = Instant.now();
    }
}
