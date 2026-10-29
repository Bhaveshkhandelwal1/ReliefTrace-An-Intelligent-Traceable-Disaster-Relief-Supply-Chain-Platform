package com.disasterreliefnet.analytics.domain;

import com.disasterreliefnet.common.enums.ShipmentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Materialised fact table for logistics throughput and SLA tracking.
 */
@Entity
@Table(name = "shipment_facts")
@Getter
@Setter
@NoArgsConstructor
public class ShipmentFact {

    @Id
    @Column(name = "fact_id", nullable = false, updatable = false)
    private UUID factId;

    @Column(name = "shipment_id", nullable = false, unique = true)
    private UUID shipmentId;

    @Column(name = "allocation_id", nullable = false)
    private UUID allocationId;

    @Column(name = "driver_id")
    private UUID driverId;

    @Column(name = "receiver_id", nullable = false)
    private UUID receiverId;

    @Column(name = "disaster_id")
    private UUID disasterId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ShipmentStatus status;

    @Column(name = "assigned_at")
    private Instant assignedAt;

    @Column(name = "picked_up_at")
    private Instant pickedUpAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "delivery_hours")
    private Double deliveryHours;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        if (factId == null) {
            factId = UUID.randomUUID();
        }
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
