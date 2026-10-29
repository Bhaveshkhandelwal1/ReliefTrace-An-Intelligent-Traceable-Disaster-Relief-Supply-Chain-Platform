package com.disasterreliefnet.finance.domain;

import com.disasterreliefnet.common.enums.DisbursementStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing an authorized financial relief disbursement or aid cash grant.
 */
@Entity
@Table(name = "disbursements")
@Getter
@Setter
@NoArgsConstructor
public class Disbursement {

    @Id
    @Column(name = "disbursement_id", nullable = false, updatable = false)
    private UUID disbursementId;

    @Column(name = "allocation_id", nullable = false, unique = true)
    private UUID allocationId;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    private DisbursementStatus status = DisbursementStatus.PENDING;

    @Column(name = "authorized_by")
    private String authorizedBy;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "paid_at")
    private Instant paidAt;

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
        if (disbursementId == null) {
            disbursementId = UUID.randomUUID();
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
