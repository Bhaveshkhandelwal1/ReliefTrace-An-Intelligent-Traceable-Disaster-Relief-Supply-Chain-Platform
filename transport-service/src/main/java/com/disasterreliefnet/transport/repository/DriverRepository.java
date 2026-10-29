package com.disasterreliefnet.transport.repository;

import com.disasterreliefnet.transport.domain.Driver;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** Spring Data JPA repository for {@link Driver} entities. */
public interface DriverRepository extends JpaRepository<Driver, UUID> {

    Optional<Driver> findByDriverIdAndDeletedAtIsNull(UUID driverId);

    Optional<Driver> findByEmailAndDeletedAtIsNull(String email);

    Optional<Driver> findByUserIdAndDeletedAtIsNull(String userId);

    Page<Driver> findAllByDeletedAtIsNull(Pageable pageable);

    Page<Driver> findAllByAvailableAndDeletedAtIsNull(boolean available, Pageable pageable);

    java.util.List<Driver> findAllByAvailableTrueAndDeletedAtIsNull();

    boolean existsByEmailAndDeletedAtIsNull(String email);

    boolean existsByUserIdAndDeletedAtIsNull(String userId);
}
