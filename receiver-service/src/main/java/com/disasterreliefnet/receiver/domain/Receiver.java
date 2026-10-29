package com.disasterreliefnet.receiver.domain;

import com.disasterreliefnet.common.enums.VerificationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Aggregate root representing a disaster victim / aid recipient profile.
 */
@Entity
@Table(name = "receivers", indexes = {
    @Index(name = "idx_receivers_email", columnList = "email"),
    @Index(name = "idx_receivers_disaster_id", columnList = "disaster_id"),
    @Index(name = "idx_receivers_verification_status", columnList = "verification_status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Receiver {

    @Id
    @Column(name = "receiver_id", nullable = false, updatable = false)
    private UUID receiverId;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "disaster_id", nullable = false)
    private UUID disasterId;

    // --- current location (embedded address) ---
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

    @Column(name = "household_size", nullable = false)
    private int householdSize;

    /** Comma-separated vulnerability tags (e.g. "ELDERLY,DISABLED"). */
    @Column(name = "vulnerability_tags", length = 500)
    private String vulnerabilityTags;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 20)
    @Builder.Default
    private VerificationStatus verificationStatus = VerificationStatus.UNVERIFIED;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @OneToMany(mappedBy = "receiver", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AidRequest> aidRequests = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (this.receiverId == null) {
            this.receiverId = UUID.randomUUID();
        }
        if (this.verificationStatus == null) {
            this.verificationStatus = VerificationStatus.UNVERIFIED;
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
}
