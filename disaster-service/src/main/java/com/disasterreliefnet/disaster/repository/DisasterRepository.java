package com.disasterreliefnet.disaster.repository;

import com.disasterreliefnet.common.enums.DisasterStatus;
import com.disasterreliefnet.common.enums.DisasterType;
import com.disasterreliefnet.disaster.domain.Disaster;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DisasterRepository extends JpaRepository<Disaster, UUID>, JpaSpecificationExecutor<Disaster> {

    Optional<Disaster> findByDisasterIdAndDeletedAtIsNull(UUID disasterId);

    Page<Disaster> findByDeletedAtIsNull(Pageable pageable);

    Page<Disaster> findByStatusAndDeletedAtIsNull(DisasterStatus status, Pageable pageable);

    Page<Disaster> findByTypeAndDeletedAtIsNull(DisasterType type, Pageable pageable);

    Page<Disaster> findByStatusAndTypeAndDeletedAtIsNull(DisasterStatus status, DisasterType type, Pageable pageable);

    boolean existsByNameIgnoreCaseAndDeletedAtIsNull(String name);
}
