package com.disasterreliefnet.verification.domain;

import com.disasterreliefnet.common.enums.DocumentType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing an uploaded supporting document for a verification case.
 */
@Entity
@Table(name = "verification_documents", indexes = {
    @Index(name = "idx_verification_documents_case_id", columnList = "case_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificationDocument {

    @Id
    @Column(name = "document_id", nullable = false, updatable = false)
    private UUID documentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_id", nullable = false)
    private VerificationCase verificationCase;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 30)
    private DocumentType documentType;

    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (this.documentId == null) {
            this.documentId = UUID.randomUUID();
        }
        Instant now = Instant.now();
        if (this.uploadedAt == null) {
            this.uploadedAt = now;
        }
        if (this.createdAt == null) {
            this.createdAt = now;
        }
    }
}
