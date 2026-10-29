package com.disasterreliefnet.analytics.repository;

import com.disasterreliefnet.analytics.domain.DisasterSummary;
import com.disasterreliefnet.common.enums.DisasterStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link DisasterSummary} entities.
 */
public interface DisasterSummaryRepository extends JpaRepository<DisasterSummary, UUID> {

    Optional<DisasterSummary> findByDisasterId(UUID disasterId);

    List<DisasterSummary> findAllByStatus(DisasterStatus status);

    long countByStatus(DisasterStatus status);
}
