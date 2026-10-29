package com.disasterreliefnet.analytics.domain;

import com.disasterreliefnet.common.enums.DonationType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Materialised fact table for incoming pledges and aid lines.
 */
@Entity
@Table(name = "donation_facts")
@Getter
@Setter
@NoArgsConstructor
public class DonationFact {

    @Id
    @Column(name = "fact_id", nullable = false, updatable = false)
    private UUID factId;

    @Column(name = "donation_id", nullable = false, unique = true)
    private UUID donationId;

    @Column(name = "donor_id", nullable = false)
    private UUID donorId;

    @Column(name = "disaster_id")
    private UUID disasterId;

    @Enumerated(EnumType.STRING)
    @Column(name = "donation_type", nullable = false, length = 20)
    private DonationType donationType;

    @Column(name = "amount", precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "quantity_kg", precision = 10, scale = 3)
    private BigDecimal quantityKg;

    @Column(name = "registered_at", nullable = false)
    private Instant registeredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (factId == null) {
            factId = UUID.randomUUID();
        }
        createdAt = Instant.now();
    }
}
