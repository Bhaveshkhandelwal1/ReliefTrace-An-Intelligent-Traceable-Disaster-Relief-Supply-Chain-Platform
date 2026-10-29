package com.disasterreliefnet.disaster.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Update and advisory posted for a specific disaster incident.
 */
@Entity
@Table(name = "disaster_updates", indexes = {
    @Index(name = "idx_disaster_updates_disaster_id", columnList = "disaster_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisasterUpdate {

    @Id
    @Column(name = "update_id", nullable = false, updatable = false)
    private UUID updateId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "disaster_id", nullable = false)
    private Disaster disaster;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", nullable = false, length = 5000)
    private String content;

    @Column(name = "source", length = 200)
    private String source;

    @Column(name = "posted_by", nullable = false, length = 255)
    private String postedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (this.updateId == null) {
            this.updateId = UUID.randomUUID();
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }
}
