package com.disasterreliefnet.transport.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity representing a relief goods shipment from a warehouse to a receiver.
 *
 * <p>Lifecycle: {@code CREATED → ASSIGNED → PICKED_UP → DELIVERED},
 * with branches to {@code DISPUTED} or {@code CANCELLED}.</p>
 */
@Entity
@Table(name = "shipments")
@Getter
@Setter
@NoArgsConstructor
public class Shipment {

    @Id
    @Column(name = "shipment_id", nullable = false, updatable = false)
    private UUID shipmentId;

    @Column(name = "allocation_id", nullable = false, unique = true)
    private UUID allocationId;

    @Column(name = "reservation_id", nullable = false)
    private UUID reservationId;

    @Column(name = "driver_id")
    private UUID driverId;

    @Column(name = "vehicle_id")
    private UUID vehicleId;

    @Column(name = "receiver_id", nullable = false)
    private UUID receiverId;

    // Pickup address (flat columns)
    @Column(name = "pickup_street", length = 500)
    private String pickupStreet;

    @Column(name = "pickup_city", length = 100)
    private String pickupCity;

    @Column(name = "pickup_state", length = 100)
    private String pickupState;

    @Column(name = "pickup_country", length = 2)
    private String pickupCountry;

    @Column(name = "pickup_zip", length = 20)
    private String pickupZip;

    // Delivery address (flat columns)
    @Column(name = "delivery_street", length = 500)
    private String deliveryStreet;

    @Column(name = "delivery_city", length = 100)
    private String deliveryCity;

    @Column(name = "delivery_state", length = 100)
    private String deliveryState;

    @Column(name = "delivery_country", length = 2)
    private String deliveryCountry;

    @Column(name = "delivery_zip", length = 20)
    private String deliveryZip;

    @Column(name = "scheduled_pickup_at")
    private Instant scheduledPickupAt;

    @Column(name = "picked_up_at")
    private Instant pickedUpAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ShipmentStatus status = ShipmentStatus.CREATED;

    @Column(name = "proof_of_delivery_url", length = 500)
    private String proofOfDeliveryUrl;

    @Column(name = "recipient_signature", length = 500)
    private String recipientSignature;

    @Column(name = "dispute_reason", length = 1000)
    private String disputeReason;

    @Column(name = "dispute_evidence_url", length = 500)
    private String disputeEvidenceUrl;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @PrePersist
    void prePersist() {
        if (shipmentId == null) {
            shipmentId = UUID.randomUUID();
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
