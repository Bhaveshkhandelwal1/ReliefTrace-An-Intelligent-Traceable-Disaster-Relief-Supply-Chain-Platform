package com.disasterreliefnet.analytics.repository;

import com.disasterreliefnet.analytics.domain.RequestFact;
import com.disasterreliefnet.common.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link RequestFact} entities.
 */
public interface RequestFactRepository extends JpaRepository<RequestFact, UUID> {

    Optional<RequestFact> findByRequestId(UUID requestId);

    @Query("""
        SELECT COUNT(r) FROM RequestFact r
        WHERE (:disasterId IS NULL OR r.disasterId = :disasterId)
          AND (:fromDate IS NULL OR r.createdAtSource >= :fromDate)
          AND (:toDate IS NULL OR r.createdAtSource <= :toDate)
    """)
    long countRequests(
        @Param("disasterId") UUID disasterId,
        @Param("fromDate") Instant fromDate,
        @Param("toDate") Instant toDate
    );

    @Query("""
        SELECT COUNT(r) FROM RequestFact r
        WHERE r.status = :status
          AND (:disasterId IS NULL OR r.disasterId = :disasterId)
          AND (:fromDate IS NULL OR r.createdAtSource >= :fromDate)
          AND (:toDate IS NULL OR r.createdAtSource <= :toDate)
    """)
    long countByStatus(
        @Param("status") RequestStatus status,
        @Param("disasterId") UUID disasterId,
        @Param("fromDate") Instant fromDate,
        @Param("toDate") Instant toDate
    );
}
