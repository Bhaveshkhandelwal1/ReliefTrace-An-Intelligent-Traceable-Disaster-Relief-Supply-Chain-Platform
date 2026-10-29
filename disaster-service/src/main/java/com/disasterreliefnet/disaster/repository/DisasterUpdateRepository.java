package com.disasterreliefnet.disaster.repository;

import com.disasterreliefnet.disaster.domain.DisasterUpdate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DisasterUpdateRepository extends JpaRepository<DisasterUpdate, UUID> {

    Page<DisasterUpdate> findByDisaster_DisasterIdOrderByCreatedAtDesc(UUID disasterId, Pageable pageable);
}
