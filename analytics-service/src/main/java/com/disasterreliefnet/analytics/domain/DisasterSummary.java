package com.disasterreliefnet.analytics.domain;

import com.disasterreliefnet.common.enums.DisasterStatus;
import com.disasterreliefnet.common.enums.DisasterType;
import com.disasterreliefnet.common.enums.SeverityLevel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Materialised aggregate summary table for high-level disaster situation reports.
 */
@Entity
@Table(name = "disaster_summaries")
@Getter
@Setter
@NoArgsConstructor
public class DisasterSummary {

    @Id
    @Column(name = "disaster_id", nullable = false)
    private UUID disasterId;

    @Column(name = "disaster_name", nullable = false, length = 200)
    private String disasterName;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private DisasterType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity_level", nullable = false, length = 15)
    private SeverityLevel severityLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    private DisasterStatus status;

    @Column(name = "total_requests_created", nullable = false)
    private long totalRequestsCreated = 0;

    @Column(name = "total_requests_delivered", nullable = false)
    private long totalRequestsDelivered = 0;

    @Column(name = "total_donations_linked", nullable = false)
    private long totalDonationsLinked = 0;

    @Column(name = "total_monetary_donated", nullable = false, precision = 20, scale = 2)
    private BigDecimal totalMonetaryDonated = BigDecimal.ZERO;

    @Column(name = "declared_at", nullable = false)
    private Instant declaredAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
