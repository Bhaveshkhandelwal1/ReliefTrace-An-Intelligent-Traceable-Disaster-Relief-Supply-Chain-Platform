package com.disasterreliefnet.matching.repository;

import com.disasterreliefnet.common.enums.AidCategory;
import com.disasterreliefnet.matching.domain.MatchedRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MatchedRequestRepository extends JpaRepository<MatchedRequest, UUID> {

    @Query("SELECT r FROM MatchedRequest r WHERE r.status = 'PENDING' ORDER BY r.priorityScore DESC")
    List<MatchedRequest> findPendingOrderedByPriority();

    @Query("SELECT r FROM MatchedRequest r WHERE r.category = :category AND r.status = 'PENDING' ORDER BY r.priorityScore DESC")
    List<MatchedRequest> findPendingByCategoryOrderedByPriority(@Param("category") AidCategory category);

    @Query("SELECT r FROM MatchedRequest r WHERE r.disasterId = :disasterId AND r.status = 'PENDING' ORDER BY r.priorityScore DESC")
    List<MatchedRequest> findPendingByDisasterId(@Param("disasterId") UUID disasterId);
}
