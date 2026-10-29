package com.disasterreliefnet.transport.repository;

import com.disasterreliefnet.transport.domain.Vehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Spring Data JPA repository for {@link Vehicle} entities. */
public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {

    Optional<Vehicle> findByVehicleIdAndDeletedAtIsNull(UUID vehicleId);

    Page<Vehicle> findAllByDeletedAtIsNull(Pageable pageable);

    Page<Vehicle> findAllByDriverIdAndDeletedAtIsNull(UUID driverId, Pageable pageable);

    List<Vehicle> findAllByDriverIdAndDeletedAtIsNull(UUID driverId);

    List<Vehicle> findAllByActiveTrueAndDeletedAtIsNull();

    Optional<Vehicle> findFirstByDriverIdAndActiveTrueAndDeletedAtIsNull(UUID driverId);

    boolean existsByPlateNumberAndDeletedAtIsNull(String plateNumber);
}
