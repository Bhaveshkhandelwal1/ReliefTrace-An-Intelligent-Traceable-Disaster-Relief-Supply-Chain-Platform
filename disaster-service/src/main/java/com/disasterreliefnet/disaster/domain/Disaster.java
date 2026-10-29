package com.disasterreliefnet.disaster.domain;

import com.disasterreliefnet.common.enums.DisasterStatus;
import com.disasterreliefnet.common.enums.DisasterType;
import com.disasterreliefnet.common.enums.SeverityLevel;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Aggregate root representing a disaster incident.
 */
@Entity
@Table(name = "disasters", indexes = {
    @Index(name = "idx_disasters_status", columnList = "status"),
    @Index(name = "idx_disasters_type", columnList = "type"),
    @Index(name = "idx_disasters_occurred_at", columnList = "occurred_at"),
    @Index(name = "idx_disasters_country", columnList = "country")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Disaster {

    @Id
    @Column(name = "disaster_id", nullable = false, updatable = false)
    private UUID disasterId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private DisasterType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity_level", nullable = false, length = 15)
    private SeverityLevel severityLevel;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @Column(name = "radius_km")
    private Double radiusKm;

    @Column(name = "region_name", length = 200)
    private String regionName;

    @Column(name = "country", length = 2)
    private String country;

    @Column(name = "estimated_affected_people", nullable = false)
    @Builder.Default
    private long estimatedAffectedPeople = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    @Builder.Default
    private DisasterStatus status = DisasterStatus.ACTIVE;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "closure_reason", length = 1000)
    private String closureReason;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @OneToMany(mappedBy = "disaster", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DisasterUpdate> updates = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (this.disasterId == null) {
            this.disasterId = UUID.randomUUID();
        }
        if (this.status == null) {
            this.status = DisasterStatus.ACTIVE;
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

    public void close(String reason) {
        this.status = DisasterStatus.CLOSED;
        this.closureReason = reason;
        this.closedAt = Instant.now();
    }
}
