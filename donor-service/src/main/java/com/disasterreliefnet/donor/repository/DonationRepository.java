package com.disasterreliefnet.donor.repository;

import com.disasterreliefnet.donor.domain.Donation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DonationRepository extends JpaRepository<Donation, UUID> {

    Optional<Donation> findByDonationIdAndDeletedAtIsNull(UUID donationId);

    Page<Donation> findByDonor_DonorIdAndDeletedAtIsNull(UUID donorId, Pageable pageable);

    Optional<Donation> findByDonationIdAndDonor_DonorIdAndDeletedAtIsNull(UUID donationId, UUID donorId);

    Optional<Donation> findByReceiptNumber(String receiptNumber);

    long countByDonor_DonorIdAndDeletedAtIsNull(UUID donorId);
}
