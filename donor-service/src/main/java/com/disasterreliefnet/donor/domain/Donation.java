package com.disasterreliefnet.donor.domain;

import com.disasterreliefnet.common.enums.DonationStatus;
import com.disasterreliefnet.common.enums.DonationType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing a monetary contribution or physical aid pledge.
 */
@Entity
@Table(name = "donations", indexes = {
    @Index(name = "idx_donations_donor_id", columnList = "donor_id"),
    @Index(name = "idx_donations_disaster_id", columnList = "disaster_id"),
    @Index(name = "idx_donations_status", columnList = "status"),
    @Index(name = "idx_donations_receipt_number", columnList = "receipt_number")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Donation {

    @Id
    @Column(name = "donation_id", nullable = false, updatable = false)
    private UUID donationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "donor_id", nullable = false)
    private Donor donor;

    @Enumerated(EnumType.STRING)
    @Column(name = "donation_type", nullable = false, length = 20)
    private DonationType donationType;

    @Column(name = "amount", precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "item_description", length = 500)
    private String itemDescription;

    @Column(name = "quantity_kg", precision = 10, scale = 3)
    private BigDecimal quantityKg;

    @Column(name = "disaster_id")
    private UUID disasterId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private DonationStatus status = DonationStatus.PENDING;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "receipt_number", length = 100)
    private String receiptNumber;

    @Column(name = "receipt_issued_at")
    private Instant receiptIssuedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @OneToMany(mappedBy = "donation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DonationItem> items = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (this.donationId == null) {
            this.donationId = UUID.randomUUID();
        }
        if (this.status == null) {
            this.status = (this.donationType == DonationType.IN_KIND)
                ? DonationStatus.PLEDGED
                : DonationStatus.PENDING;
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

    public void addItem(DonationItem item) {
        items.add(item);
        item.setDonation(this);
    }

    public void generateReceipt(String receiptPrefix) {
        this.receiptNumber = receiptPrefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.receiptIssuedAt = Instant.now();
    }
}
