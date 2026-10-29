package com.disasterreliefnet.receiver.domain;

import com.disasterreliefnet.common.enums.AidCategory;
import com.disasterreliefnet.common.enums.RequestStatus;
import com.disasterreliefnet.common.enums.UrgencyLevel;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Aggregate representing a single aid request submitted by a receiver.
 */
@Entity
@Table(name = "aid_requests", indexes = {
    @Index(name = "idx_aid_requests_receiver_id", columnList = "receiver_id"),
    @Index(name = "idx_aid_requests_disaster_id", columnList = "disaster_id"),
    @Index(name = "idx_aid_requests_status", columnList = "status"),
    @Index(name = "idx_aid_requests_urgency", columnList = "urgency_level")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AidRequest {

    @Id
    @Column(name = "request_id", nullable = false, updatable = false)
    private UUID requestId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receiver_id", nullable = false)
    private Receiver receiver;

    @Column(name = "disaster_id", nullable = false)
    private UUID disasterId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private AidCategory category;

    @Column(name = "description", nullable = false, length = 1000)
    private String description;

    @Column(name = "quantity_requested", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantityRequested;

    @Column(name = "unit", nullable = false, length = 20)
    private String unit;

    @Enumerated(EnumType.STRING)
    @Column(name = "urgency_level", nullable = false, length = 20)
    private UrgencyLevel urgencyLevel;

    // --- delivery address (embedded) ---
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

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private RequestStatus status = RequestStatus.PENDING;

    /** Set after matching/allocation is approved. */
    @Column(name = "allocation_id")
    private UUID allocationId;

    /** Fingerprint for duplicate-request detection: hash of receiverId + disasterId + category + unit. */
    @Column(name = "dedup_key", length = 100)
    private String dedupKey;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @PrePersist
    public void prePersist() {
        if (this.requestId == null) {
            this.requestId = UUID.randomUUID();
        }
        if (this.status == null) {
            this.status = RequestStatus.PENDING;
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = Instant.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
