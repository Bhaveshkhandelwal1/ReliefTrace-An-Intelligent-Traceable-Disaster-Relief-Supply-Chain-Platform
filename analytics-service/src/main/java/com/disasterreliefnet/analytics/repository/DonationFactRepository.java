package com.disasterreliefnet.analytics.repository;

import com.disasterreliefnet.analytics.domain.DonationFact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link DonationFact} entities.
 */
public interface DonationFactRepository extends JpaRepository<DonationFact, UUID> {

    Optional<DonationFact> findByDonationId(UUID donationId);

    @Query("""
        SELECT COUNT(d) FROM DonationFact d
        WHERE (:disasterId IS NULL OR d.disasterId = :disasterId)
          AND (:fromDate IS NULL OR d.registeredAt >= :fromDate)
          AND (:toDate IS NULL OR d.registeredAt <= :toDate)
    """)
    long countDonations(
        @Param("disasterId") UUID disasterId,
        @Param("fromDate") Instant fromDate,
        @Param("toDate") Instant toDate
    );

    @Query("""
        SELECT COALESCE(SUM(d.amount), 0) FROM DonationFact d
        WHERE d.donationType = 'MONETARY'
          AND (:disasterId IS NULL OR d.disasterId = :disasterId)
          AND (:fromDate IS NULL OR d.registeredAt >= :fromDate)
          AND (:toDate IS NULL OR d.registeredAt <= :toDate)
    """)
    BigDecimal sumMonetaryAmount(
        @Param("disasterId") UUID disasterId,
        @Param("fromDate") Instant fromDate,
        @Param("toDate") Instant toDate
    );

    @Query("""
        SELECT COALESCE(SUM(d.quantityKg), 0) FROM DonationFact d
        WHERE d.donationType = 'IN_KIND'
          AND (:disasterId IS NULL OR d.disasterId = :disasterId)
          AND (:fromDate IS NULL OR d.registeredAt >= :fromDate)
          AND (:toDate IS NULL OR d.registeredAt <= :toDate)
    """)
    BigDecimal sumInKindKg(
        @Param("disasterId") UUID disasterId,
        @Param("fromDate") Instant fromDate,
        @Param("toDate") Instant toDate
    );
}
