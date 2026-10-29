package com.disasterreliefnet.transport.repository;

import com.disasterreliefnet.transport.domain.Shipment;
import com.disasterreliefnet.transport.domain.ShipmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Spring Data JPA repository for {@link Shipment} entities. */
public interface ShipmentRepository extends JpaRepository<Shipment, UUID> {

    Optional<Shipment> findByShipmentIdAndDeletedAtIsNull(UUID shipmentId);

    Optional<Shipment> findByAllocationIdAndDeletedAtIsNull(UUID allocationId);

    Page<Shipment> findAllByDeletedAtIsNull(Pageable pageable);

    Page<Shipment> findAllByStatusAndDeletedAtIsNull(ShipmentStatus status, Pageable pageable);

    Page<Shipment> findAllByDriverIdAndDeletedAtIsNull(UUID driverId, Pageable pageable);

    Page<Shipment> findAllByDriverIdAndStatusAndDeletedAtIsNull(UUID driverId, ShipmentStatus status, Pageable pageable);

    Page<Shipment> findAllByReceiverIdAndDeletedAtIsNull(UUID receiverId, Pageable pageable);

    List<Shipment> findAllByVehicleIdAndStatusInAndDeletedAtIsNull(UUID vehicleId, List<ShipmentStatus> statuses);

    List<Shipment> findAllByStatusInAndDeletedAtIsNull(List<ShipmentStatus> statuses);

    /** Finds CREATED shipments past their scheduled pick-up to detect SLA breaches. */
    @Query("SELECT s FROM Shipment s WHERE s.status = 'CREATED' AND s.scheduledPickupAt < :threshold AND s.deletedAt IS NULL")
    List<Shipment> findUnassignedPastThreshold(@Param("threshold") Instant threshold);
}
