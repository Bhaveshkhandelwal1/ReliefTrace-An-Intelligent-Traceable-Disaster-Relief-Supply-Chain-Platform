package com.disasterreliefnet.verification.repository;

import com.disasterreliefnet.common.enums.CaseStatus;
import com.disasterreliefnet.verification.domain.VerificationCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VerificationCaseRepository extends JpaRepository<VerificationCase, UUID> {

    @Query("SELECT c FROM VerificationCase c WHERE c.caseId = :caseId AND c.deletedAt IS NULL")
    Optional<VerificationCase> findActiveById(@Param("caseId") UUID caseId);

    @Query("SELECT c FROM VerificationCase c WHERE c.requestId = :requestId AND c.deletedAt IS NULL")
    Optional<VerificationCase> findByRequestId(@Param("requestId") UUID requestId);

    @Query("SELECT c FROM VerificationCase c WHERE c.deletedAt IS NULL")
    Page<VerificationCase> findAllActive(Pageable pageable);

    @Query("SELECT c FROM VerificationCase c WHERE c.status = :status AND c.deletedAt IS NULL")
    Page<VerificationCase> findAllByStatus(@Param("status") CaseStatus status, Pageable pageable);

    boolean existsByRequestIdAndDeletedAtIsNull(UUID requestId);
}
