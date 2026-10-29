package com.disasterreliefnet.receiver.repository;

import com.disasterreliefnet.common.enums.RequestStatus;
import com.disasterreliefnet.receiver.domain.AidRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link AidRequest} entities.
 */
public interface AidRequestRepository extends JpaRepository<AidRequest, UUID> {

    @Query("SELECT a FROM AidRequest a WHERE a.receiver.receiverId = :receiverId AND a.deletedAt IS NULL")
    Page<AidRequest> findAllByReceiverId(@Param("receiverId") UUID receiverId, Pageable pageable);

    @Query("SELECT a FROM AidRequest a WHERE a.receiver.receiverId = :receiverId AND a.status = :status AND a.deletedAt IS NULL")
    Page<AidRequest> findAllByReceiverIdAndStatus(
        @Param("receiverId") UUID receiverId,
        @Param("status") RequestStatus status,
        Pageable pageable
    );

    @Query("SELECT a FROM AidRequest a WHERE a.deletedAt IS NULL")
    Page<AidRequest> findAllActive(Pageable pageable);

    @Query("SELECT a FROM AidRequest a WHERE a.disasterId = :disasterId AND a.deletedAt IS NULL")
    Page<AidRequest> findAllByDisasterId(@Param("disasterId") UUID disasterId, Pageable pageable);

    @Query("SELECT a FROM AidRequest a WHERE a.disasterId = :disasterId AND a.status = :status AND a.deletedAt IS NULL")
    Page<AidRequest> findAllByDisasterIdAndStatus(
        @Param("disasterId") UUID disasterId,
        @Param("status") RequestStatus status,
        Pageable pageable
    );

    @Query("SELECT a FROM AidRequest a WHERE a.requestId = :requestId AND a.receiver.receiverId = :receiverId AND a.deletedAt IS NULL")
    Optional<AidRequest> findByRequestIdAndReceiverId(
        @Param("requestId") UUID requestId,
        @Param("receiverId") UUID receiverId
    );

    boolean existsByDedupKeyAndDeletedAtIsNull(String dedupKey);
}
