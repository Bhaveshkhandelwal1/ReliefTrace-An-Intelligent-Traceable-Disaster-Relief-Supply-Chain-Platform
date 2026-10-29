package com.disasterreliefnet.matching.domain;

import com.disasterreliefnet.common.enums.AidCategory;
import com.disasterreliefnet.common.enums.UrgencyLevel;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Entity maintaining state and priority metadata for verified aid requests awaiting matching.
 */
@Entity
@Table(name = "matched_requests", indexes = {
    @Index(name = "idx_matched_requests_disaster_id", columnList = "disaster_id"),
    @Index(name = "idx_matched_requests_category", columnList = "category"),
    @Index(name = "idx_matched_requests_priority", columnList = "priority_score"),
    @Index(name = "idx_matched_requests_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchedRequest {

    @Id
    @Column(name = "request_id", nullable = false, updatable = false)
    private UUID requestId;

    @Column(name = "receiver_id", nullable = false)
    private UUID receiverId;

    @Column(name = "disaster_id", nullable = false)
    private UUID disasterId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private AidCategory category;

    @Column(name = "quantity_requested", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantityRequested;

    @Column(name = "quantity_allocated", nullable = false, precision = 12, scale = 3)
    @Builder.Default
    private BigDecimal quantityAllocated = BigDecimal.ZERO;

    @Column(name = "unit", nullable = false, length = 20)
    private String unit;

    @Enumerated(EnumType.STRING)
    @Column(name = "urgency_level", nullable = false, length = 20)
    private UrgencyLevel urgencyLevel;

    @Column(name = "household_size", nullable = false)
    @Builder.Default
    private int householdSize = 1;

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

    @Column(name = "priority_score", nullable = false)
    @Builder.Default
    private double priorityScore = 0.0;

    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "verified_at", nullable = false)
    private Instant verifiedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        if (this.verifiedAt == null) {
            this.verifiedAt = now;
        }
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
