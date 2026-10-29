package com.disasterreliefnet.analytics.domain;

import com.disasterreliefnet.common.enums.AidCategory;
import com.disasterreliefnet.common.enums.RequestStatus;
import com.disasterreliefnet.common.enums.UrgencyLevel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Materialised fact table for victim aid requests and status progressions.
 */
@Entity
@Table(name = "request_facts")
@Getter
@Setter
@NoArgsConstructor
public class RequestFact {

    @Id
    @Column(name = "fact_id", nullable = false, updatable = false)
    private UUID factId;

    @Column(name = "request_id", nullable = false, unique = true)
    private UUID requestId;

    @Column(name = "receiver_id", nullable = false)
    private UUID receiverId;

    @Column(name = "disaster_id", nullable = false)
    private UUID disasterId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private AidCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "urgency_level", nullable = false, length = 10)
    private UrgencyLevel urgencyLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RequestStatus status;

    @Column(name = "quantity_requested", precision = 10, scale = 3)
    private BigDecimal quantityRequested;

    @Column(name = "unit", length = 20)
    private String unit;

    @Column(name = "created_at_source", nullable = false)
    private Instant createdAtSource;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

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
