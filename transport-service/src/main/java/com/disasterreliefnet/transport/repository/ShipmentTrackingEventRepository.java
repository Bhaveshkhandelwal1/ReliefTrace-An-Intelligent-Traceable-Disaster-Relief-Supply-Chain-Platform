package com.disasterreliefnet.transport.repository;

import com.disasterreliefnet.transport.domain.ShipmentTrackingEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Spring Data JPA repository for {@link ShipmentTrackingEvent} entities. */
public interface ShipmentTrackingEventRepository extends JpaRepository<ShipmentTrackingEvent, UUID> {

    List<ShipmentTrackingEvent> findAllByShipmentIdOrderByOccurredAtAsc(UUID shipmentId);
}
