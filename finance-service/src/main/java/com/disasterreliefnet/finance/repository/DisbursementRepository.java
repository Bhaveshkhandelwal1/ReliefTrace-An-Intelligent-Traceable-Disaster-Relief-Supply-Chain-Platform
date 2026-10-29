package com.disasterreliefnet.finance.repository;

import com.disasterreliefnet.common.enums.DisbursementStatus;
import com.disasterreliefnet.finance.domain.Disbursement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Disbursement} entities.
 */
public interface DisbursementRepository extends JpaRepository<Disbursement, UUID> {

    Optional<Disbursement> findByDisbursementIdAndDeletedAtIsNull(UUID disbursementId);

    Optional<Disbursement> findByAllocationIdAndDeletedAtIsNull(UUID allocationId);

    Page<Disbursement> findAllByDeletedAtIsNull(Pageable pageable);

    Page<Disbursement> findAllByAllocationIdAndDeletedAtIsNull(UUID allocationId, Pageable pageable);

    Page<Disbursement> findAllByStatusAndDeletedAtIsNull(DisbursementStatus status, Pageable pageable);

    boolean existsByAllocationIdAndDeletedAtIsNull(UUID allocationId);
}
