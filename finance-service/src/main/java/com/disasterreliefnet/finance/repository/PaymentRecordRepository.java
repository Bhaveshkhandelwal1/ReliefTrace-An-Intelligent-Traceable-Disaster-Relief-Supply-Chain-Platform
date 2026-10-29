package com.disasterreliefnet.finance.repository;

import com.disasterreliefnet.common.enums.PaymentStatus;
import com.disasterreliefnet.finance.domain.PaymentRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link PaymentRecord} entities.
 */
public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, UUID> {

    Optional<PaymentRecord> findByPaymentIdAndDeletedAtIsNull(UUID paymentId);

    Optional<PaymentRecord> findByDonationIdAndDeletedAtIsNull(UUID donationId);

    Page<PaymentRecord> findAllByDeletedAtIsNull(Pageable pageable);

    Page<PaymentRecord> findAllByStatusAndDeletedAtIsNull(PaymentStatus status, Pageable pageable);

    Page<PaymentRecord> findAllByDonorIdAndDeletedAtIsNull(UUID donorId, Pageable pageable);

    Page<PaymentRecord> findAllByDonationIdAndDeletedAtIsNull(UUID donationId, Pageable pageable);

    boolean existsByDonationIdAndDeletedAtIsNull(UUID donationId);
}
