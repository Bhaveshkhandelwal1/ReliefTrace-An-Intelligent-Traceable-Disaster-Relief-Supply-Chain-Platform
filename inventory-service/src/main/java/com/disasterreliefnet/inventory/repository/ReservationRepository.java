package com.disasterreliefnet.inventory.repository;

import com.disasterreliefnet.common.enums.ReservationStatus;
import com.disasterreliefnet.inventory.domain.Reservation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    Optional<Reservation> findByReservationIdAndDeletedAtIsNull(UUID reservationId);

    Optional<Reservation> findByAllocationIdAndDeletedAtIsNull(UUID allocationId);

    Page<Reservation> findByDeletedAtIsNull(Pageable pageable);

    Page<Reservation> findByAllocationIdAndDeletedAtIsNull(UUID allocationId, Pageable pageable);

    List<Reservation> findByStatusInAndCreatedAtBeforeAndDeletedAtIsNull(Collection<ReservationStatus> statuses, Instant cutoff);
}
