package com.disasterreliefnet.donor.repository;

import com.disasterreliefnet.common.enums.DonorStatus;
import com.disasterreliefnet.donor.domain.Donor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DonorRepository extends JpaRepository<Donor, UUID>, JpaSpecificationExecutor<Donor> {

    Optional<Donor> findByDonorIdAndDeletedAtIsNull(UUID donorId);

    Optional<Donor> findByEmailIgnoreCaseAndDeletedAtIsNull(String email);

    boolean existsByEmailIgnoreCaseAndDeletedAtIsNull(String email);

    Page<Donor> findByDeletedAtIsNull(Pageable pageable);

    Page<Donor> findByStatusAndDeletedAtIsNull(DonorStatus status, Pageable pageable);
}
