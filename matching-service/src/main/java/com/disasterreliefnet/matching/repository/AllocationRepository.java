package com.disasterreliefnet.matching.repository;

import com.disasterreliefnet.common.enums.AllocationStatus;
import com.disasterreliefnet.matching.domain.Allocation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AllocationRepository extends JpaRepository<Allocation, UUID> {

    @Query("SELECT a FROM Allocation a WHERE a.allocationId = :allocationId AND a.deletedAt IS NULL")
    Optional<Allocation> findActiveById(@Param("allocationId") UUID allocationId);

    @Query("SELECT a FROM Allocation a WHERE a.requestId = :requestId AND a.deletedAt IS NULL")
    List<Allocation> findAllByRequestId(@Param("requestId") UUID requestId);

    @Query("SELECT a FROM Allocation a WHERE a.deletedAt IS NULL")
    Page<Allocation> findAllActive(Pageable pageable);

    @Query("SELECT a FROM Allocation a WHERE a.status = :status AND a.deletedAt IS NULL")
    Page<Allocation> findAllByStatus(@Param("status") AllocationStatus status, Pageable pageable);

    @Query("SELECT a FROM Allocation a WHERE a.disasterId = :disasterId AND a.deletedAt IS NULL")
    Page<Allocation> findAllByDisasterId(@Param("disasterId") UUID disasterId, Pageable pageable);

    @Query("SELECT a FROM Allocation a WHERE a.status = :status AND a.disasterId = :disasterId AND a.deletedAt IS NULL")
    Page<Allocation> findAllByStatusAndDisasterId(
        @Param("status") AllocationStatus status,
        @Param("disasterId") UUID disasterId,
        Pageable pageable
    );

    @Query("SELECT a FROM Allocation a WHERE a.status = :status AND a.createdAt < :cutoff AND a.deletedAt IS NULL")
    List<Allocation> findStaleAllocations(@Param("status") AllocationStatus status, @Param("cutoff") Instant cutoff);
}
