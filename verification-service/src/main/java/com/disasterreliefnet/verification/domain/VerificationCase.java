package com.disasterreliefnet.verification.domain;

import com.disasterreliefnet.common.enums.CaseStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Aggregate root representing a verification case for an aid request or recipient.
 */
@Entity
@Table(name = "verification_cases", indexes = {
    @Index(name = "idx_verification_cases_request_id", columnList = "request_id", unique = true),
    @Index(name = "idx_verification_cases_receiver_id", columnList = "receiver_id"),
    @Index(name = "idx_verification_cases_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificationCase {

    @Id
    @Column(name = "case_id", nullable = false, updatable = false)
    private UUID caseId;

    @Column(name = "request_id", nullable = false, unique = true)
    private UUID requestId;

    @Column(name = "receiver_id", nullable = false)
    private UUID receiverId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private CaseStatus status = CaseStatus.OPEN;

    @Column(name = "reviewed_by", length = 255)
    private String reviewedBy;

    @Column(name = "review_notes", length = 1000)
    private String reviewNotes;

    @Column(name = "opened_at", nullable = false, updatable = false)
    private Instant openedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @OneToMany(mappedBy = "verificationCase", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<VerificationDocument> documents = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (this.caseId == null) {
            this.caseId = UUID.randomUUID();
        }
        if (this.status == null) {
            this.status = CaseStatus.OPEN;
        }
        Instant now = Instant.now();
        if (this.openedAt == null) {
            this.openedAt = now;
        }
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public void addDocument(VerificationDocument document) {
        documents.add(document);
        document.setVerificationCase(this);
    }
}
