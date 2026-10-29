package com.disasterreliefnet.matching.domain;

import com.disasterreliefnet.common.enums.AllocationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Aggregate root representing an inventory allocation matched to an aid request.
 */
@Entity
@Table(name = "allocations", indexes = {
    @Index(name = "idx_allocations_request_id", columnList = "request_id"),
    @Index(name = "idx_allocations_item_id", columnList = "item_id"),
    @Index(name = "idx_allocations_status", columnList = "status"),
    @Index(name = "idx_allocations_disaster_id", columnList = "disaster_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Allocation {

    @Id
    @Column(name = "allocation_id", nullable = false, updatable = false)
    private UUID allocationId;

    @Column(name = "request_id", nullable = false)
    private UUID requestId;

    @Column(name = "receiver_id", nullable = false)
    private UUID receiverId;

    @Column(name = "disaster_id", nullable = false)
    private UUID disasterId;

    @Column(name = "item_id", nullable = false)
    private UUID itemId;

    @Column(name = "warehouse_id", nullable = false)
    private UUID warehouseId;

    @Column(name = "quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    @Column(name = "unit", nullable = false, length = 20)
    @Builder.Default
    private String unit = "units";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private AllocationStatus status = AllocationStatus.SUGGESTED;

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

    @Column(name = "approved_by", length = 255)
    private String approvedBy;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

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
        if (this.allocationId == null) {
            this.allocationId = UUID.randomUUID();
        }
        if (this.status == null) {
            this.status = AllocationStatus.SUGGESTED;
        }
        Instant now = Instant.now();
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
