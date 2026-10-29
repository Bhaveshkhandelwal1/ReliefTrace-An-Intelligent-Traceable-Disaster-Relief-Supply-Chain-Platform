package com.disasterreliefnet.analytics.repository;

import com.disasterreliefnet.analytics.domain.ShipmentFact;
import com.disasterreliefnet.common.enums.ShipmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link ShipmentFact} entities.
 */
public interface ShipmentFactRepository extends JpaRepository<ShipmentFact, UUID> {

    Optional<ShipmentFact> findByShipmentId(UUID shipmentId);

    @Query("""
        SELECT COUNT(s) FROM ShipmentFact s
        WHERE (:disasterId IS NULL OR s.disasterId = :disasterId)
          AND (:fromDate IS NULL OR s.createdAt >= :fromDate)
          AND (:toDate IS NULL OR s.createdAt <= :toDate)
    """)
    long countShipments(
        @Param("disasterId") UUID disasterId,
        @Param("fromDate") Instant fromDate,
        @Param("toDate") Instant toDate
    );

    @Query("""
        SELECT COUNT(s) FROM ShipmentFact s
        WHERE s.status = :status
          AND (:disasterId IS NULL OR s.disasterId = :disasterId)
          AND (:fromDate IS NULL OR s.createdAt >= :fromDate)
          AND (:toDate IS NULL OR s.createdAt <= :toDate)
    """)
    long countByStatus(
        @Param("status") ShipmentStatus status,
        @Param("disasterId") UUID disasterId,
        @Param("fromDate") Instant fromDate,
        @Param("toDate") Instant toDate
    );

    @Query("""
        SELECT COALESCE(AVG(s.deliveryHours), 0.0) FROM ShipmentFact s
        WHERE s.status = 'DELIVERED'
          AND s.deliveryHours IS NOT NULL
          AND (:disasterId IS NULL OR s.disasterId = :disasterId)
          AND (:fromDate IS NULL OR s.createdAt >= :fromDate)
          AND (:toDate IS NULL OR s.createdAt <= :toDate)
    """)
    double avgDeliveryHours(
        @Param("disasterId") UUID disasterId,
        @Param("fromDate") Instant fromDate,
        @Param("toDate") Instant toDate
    );
}
