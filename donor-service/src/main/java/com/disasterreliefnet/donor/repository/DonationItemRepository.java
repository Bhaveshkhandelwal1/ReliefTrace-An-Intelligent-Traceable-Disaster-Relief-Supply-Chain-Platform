package com.disasterreliefnet.donor.repository;

import com.disasterreliefnet.donor.domain.DonationItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DonationItemRepository extends JpaRepository<DonationItem, UUID> {

    List<DonationItem> findByDonation_DonationId(UUID donationId);
}
