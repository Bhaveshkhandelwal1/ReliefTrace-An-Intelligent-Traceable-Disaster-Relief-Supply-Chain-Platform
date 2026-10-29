package com.disasterreliefnet.donor.domain;

import com.disasterreliefnet.common.enums.DonorStatus;
import com.disasterreliefnet.common.enums.DonorType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Aggregate root representing a donor profile and impact metrics.
 */
@Entity
@Table(name = "donors", indexes = {
    @Index(name = "idx_donors_email", columnList = "email", unique = true),
    @Index(name = "idx_donors_status", columnList = "status"),
    @Index(name = "idx_donors_type", columnList = "donor_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Donor {

    @Id
    @Column(name = "donor_id", nullable = false, updatable = false)
    private UUID donorId;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "donor_type", nullable = false, length = 20)
    @Builder.Default
    private DonorType donorType = DonorType.INDIVIDUAL;

    @Column(name = "street", length = 500)
    private String street;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "country", length = 2)
    private String country;

    @Column(name = "zip", length = 20)
    private String zip;

    @Column(name = "tax_id", length = 50)
    private String taxId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private DonorStatus status = DonorStatus.ACTIVE;

    @Column(name = "total_donated_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalDonatedAmount = BigDecimal.ZERO;

    @Column(name = "impact_deliveries_count", nullable = false)
    @Builder.Default
    private long impactDeliveriesCount = 0L;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @OneToMany(mappedBy = "donor", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Donation> donations = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (this.donorId == null) {
            this.donorId = UUID.randomUUID();
        }
        if (this.donorType == null) {
            this.donorType = DonorType.INDIVIDUAL;
        }
        if (this.status == null) {
            this.status = DonorStatus.ACTIVE;
        }
        if (this.totalDonatedAmount == null) {
            this.totalDonatedAmount = BigDecimal.ZERO;
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

    public void incrementImpactDeliveries() {
        this.impactDeliveriesCount++;
    }

    public void recordContribution(BigDecimal amount) {
        if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
            this.totalDonatedAmount = this.totalDonatedAmount.add(amount);
        }
    }
}
