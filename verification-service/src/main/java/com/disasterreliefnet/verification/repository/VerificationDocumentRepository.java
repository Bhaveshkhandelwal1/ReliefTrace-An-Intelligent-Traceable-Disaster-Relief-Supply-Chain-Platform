package com.disasterreliefnet.verification.repository;

import com.disasterreliefnet.verification.domain.VerificationDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VerificationDocumentRepository extends JpaRepository<VerificationDocument, UUID> {

    List<VerificationDocument> findAllByVerificationCase_CaseId(UUID caseId);
}
