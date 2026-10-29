package com.disasterreliefnet.donor.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Line item for in-kind aid pledges and physical donations.
 */
@Entity
@Table(name = "donation_items", indexes = {
    @Index(name = "idx_donation_items_donation_id", columnList = "donation_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DonationItem {

    @Id
    @Column(name = "item_line_id", nullable = false, updatable = false)
    private UUID itemLineId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "donation_id", nullable = false)
    private Donation donation;

    @Column(name = "item_name", nullable = false, length = 200)
    private String itemName;

    @Column(name = "category", nullable = false, length = 30)
    private String category;

    @Column(name = "quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    @Column(name = "unit", nullable = false, length = 30)
    private String unit;

    @Column(name = "estimated_value", precision = 15, scale = 2)
    private BigDecimal estimatedValue;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (this.itemLineId == null) {
            this.itemLineId = UUID.randomUUID();
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }
}
