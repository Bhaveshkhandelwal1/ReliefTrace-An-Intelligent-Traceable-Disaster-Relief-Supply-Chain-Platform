package com.disasterreliefnet.transport.service;

import com.disasterreliefnet.common.event.KafkaTopics;
import com.disasterreliefnet.common.event.payload.*;
import com.disasterreliefnet.common.exception.BusinessRuleException;
import com.disasterreliefnet.common.exception.ConflictException;
import com.disasterreliefnet.common.exception.ForbiddenException;
import com.disasterreliefnet.common.exception.ResourceNotFoundException;
import com.disasterreliefnet.common.outbox.OutboxService;
import com.disasterreliefnet.transport.client.DisasterClient;
import com.disasterreliefnet.transport.client.DisasterClientDto;
import com.disasterreliefnet.transport.domain.*;
import com.disasterreliefnet.transport.dto.*;
import com.disasterreliefnet.transport.mapper.TransportMapper;
import com.disasterreliefnet.transport.repository.DriverRepository;
import com.disasterreliefnet.transport.repository.ShipmentRepository;
import com.disasterreliefnet.transport.repository.ShipmentTrackingEventRepository;
import com.disasterreliefnet.transport.repository.VehicleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service managing shipment lifecycle, driver assignments, capacity matching,
 * tracking updates, breakdown & road closure reassignments, and outbox event dispatch.
 */
@Service
@Transactional(readOnly = true)
public class ShipmentService {

    private static final Logger log = LoggerFactory.getLogger(ShipmentService.class);

    private final ShipmentRepository shipmentRepository;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final ShipmentTrackingEventRepository trackingEventRepository;
    private final OutboxService outboxService;
    private final TransportMapper transportMapper;
    private final DisasterClient disasterClient;

    public ShipmentService(
        ShipmentRepository shipmentRepository,
        DriverRepository driverRepository,
        VehicleRepository vehicleRepository,
        ShipmentTrackingEventRepository trackingEventRepository,
        OutboxService outboxService,
        TransportMapper transportMapper,
        DisasterClient disasterClient
    ) {
        this.shipmentRepository = shipmentRepository;
        this.driverRepository = driverRepository;
        this.vehicleRepository = vehicleRepository;
        this.trackingEventRepository = trackingEventRepository;
        this.outboxService = outboxService;
        this.transportMapper = transportMapper;
        this.disasterClient = disasterClient;
    }

    @Transactional
    public ShipmentResponse createShipment(CreateShipmentRequest request) {
        log.info("Creating shipment for allocation ID: {}", request.allocationId());

        if (shipmentRepository.findByAllocationIdAndDeletedAtIsNull(request.allocationId()).isPresent()) {
            throw new ConflictException("Shipment for allocation " + request.allocationId() + " already exists");
        }

        Shipment shipment = transportMapper.toShipment(request);
        shipment.setStatus(ShipmentStatus.CREATED);
        Shipment saved = shipmentRepository.save(shipment);

        recordTrackingEvent(saved.getShipmentId(), "CREATED", "Shipment created and awaiting driver dispatch");

        log.info("Shipment created with ID: {}", saved.getShipmentId());
        return transportMapper.toShipmentResponse(saved);
    }

    @Transactional
    public void createShipmentFromStockReserved(StockReservedPayload payload) {
        log.info("Creating shipment from stock reserved event for allocation: {}", payload.allocationId());

        if (shipmentRepository.findByAllocationIdAndDeletedAtIsNull(payload.allocationId()).isPresent()) {
            log.info("Shipment for allocation {} already exists, ignoring duplicate event", payload.allocationId());
            return;
        }

        Shipment shipment = new Shipment();
        shipment.setAllocationId(payload.allocationId());
        shipment.setReservationId(payload.reservationId());
        shipment.setReceiverId(payload.receiverId());

        if (payload.pickupAddress() != null) {
            shipment.setPickupStreet(payload.pickupAddress().street());
            shipment.setPickupCity(payload.pickupAddress().city());
            shipment.setPickupState(payload.pickupAddress().state());
            shipment.setPickupCountry(payload.pickupAddress().country());
            shipment.setPickupZip(payload.pickupAddress().zip());
        }

        if (payload.deliveryAddress() != null) {
            shipment.setDeliveryStreet(payload.deliveryAddress().street());
            shipment.setDeliveryCity(payload.deliveryAddress().city());
            shipment.setDeliveryState(payload.deliveryAddress().state());
            shipment.setDeliveryCountry(payload.deliveryAddress().country());
            shipment.setDeliveryZip(payload.deliveryAddress().zip());
        }

        shipment.setScheduledPickupAt(Instant.now().plus(2, ChronoUnit.HOURS));
        shipment.setStatus(ShipmentStatus.CREATED);

        Shipment saved = shipmentRepository.save(shipment);
        recordTrackingEvent(saved.getShipmentId(), "CREATED", "Shipment created from warehouse reservation " + payload.reservationId());
        log.info("Shipment created via stock reservation event with ID: {}", saved.getShipmentId());
    }

    @Transactional
    public void cancelShipmentByAllocation(UUID allocationId, String reason) {
        log.info("Cancelling shipment for allocation: {} due to: {}", allocationId, reason);

        shipmentRepository.findByAllocationIdAndDeletedAtIsNull(allocationId).ifPresent(shipment -> {
            if (shipment.getStatus() == ShipmentStatus.DELIVERED || shipment.getStatus() == ShipmentStatus.CANCELLED) {
                log.info("Shipment {} is already in terminal state {}, skipping cancellation", shipment.getShipmentId(), shipment.getStatus());
                return;
            }

            // Release driver if assigned
            if (shipment.getDriverId() != null) {
                driverRepository.findByDriverIdAndDeletedAtIsNull(shipment.getDriverId()).ifPresent(driver -> {
                    if (shipment.getShipmentId().equals(driver.getActiveShipmentId())) {
                        driver.setAvailable(true);
                        driver.setActiveShipmentId(null);
                        driverRepository.save(driver);
                    }
                });
            }

            shipment.setStatus(ShipmentStatus.CANCELLED);
            shipment.setDisputeReason(reason);
            shipmentRepository.save(shipment);

            recordTrackingEvent(shipment.getShipmentId(), "CANCELLED", reason);

            ShipmentCancelledPayload payload = new ShipmentCancelledPayload(
                shipment.getShipmentId(),
                shipment.getAllocationId(),
                shipment.getReservationId(),
                Instant.now(),
                reason
            );

            outboxService.enqueue(
                KafkaTopics.SHIPMENT_CANCELLED,
                "SHIPMENT",
                shipment.getShipmentId(),
                "ShipmentCancelled",
                payload,
                null
            );
        });
    }

    @Transactional
    public ShipmentResponse assignDriver(UUID shipmentId, AssignDriverRequest request) {
        log.info("Assigning driver: {} and vehicle: {} to shipment: {}", request.driverId(), request.vehicleId(), shipmentId);

        Shipment shipment = findShipmentOrThrow(shipmentId);

        if (shipment.getStatus() != ShipmentStatus.CREATED && shipment.getStatus() != ShipmentStatus.ASSIGNED) {
            throw new BusinessRuleException("Cannot assign driver to shipment in status: " + shipment.getStatus());
        }

        Driver driver = driverRepository.findByDriverIdAndDeletedAtIsNull(request.driverId())
            .orElseThrow(() -> new ResourceNotFoundException("Driver", request.driverId()));

        if (!driver.isAvailable() && !request.driverId().equals(shipment.getDriverId())) {
            throw new BusinessRuleException("Driver " + request.driverId() + " is currently unavailable");
        }

        Vehicle vehicle = vehicleRepository.findByVehicleIdAndDeletedAtIsNull(request.vehicleId())
            .orElseThrow(() -> new ResourceNotFoundException("Vehicle", request.vehicleId()));

        if (!vehicle.isActive()) {
            throw new BusinessRuleException("Vehicle " + request.vehicleId() + " is currently inactive");
        }

        if (!driver.getDriverId().equals(vehicle.getDriverId())) {
            throw new BusinessRuleException("Vehicle " + request.vehicleId() + " is not registered to driver " + request.driverId());
        }

        // Release old driver if reassignment
        if (shipment.getDriverId() != null && !shipment.getDriverId().equals(driver.getDriverId())) {
            driverRepository.findByDriverIdAndDeletedAtIsNull(shipment.getDriverId()).ifPresent(oldDriver -> {
                oldDriver.setAvailable(true);
                oldDriver.setActiveShipmentId(null);
                driverRepository.save(oldDriver);
            });
        }

        shipment.setDriverId(driver.getDriverId());
        shipment.setVehicleId(vehicle.getVehicleId());
        shipment.setStatus(ShipmentStatus.ASSIGNED);
        Shipment savedShipment = shipmentRepository.save(shipment);

        driver.setAvailable(false);
        driver.setActiveShipmentId(savedShipment.getShipmentId());
        driverRepository.save(driver);

        String desc = "Assigned to driver " + driver.getFirstName() + " " + driver.getLastName() + " (Vehicle: " + vehicle.getPlateNumber() + ")";
        recordTrackingEvent(savedShipment.getShipmentId(), "ASSIGNED", desc);

        ShipmentAssignedPayload payload = new ShipmentAssignedPayload(
            savedShipment.getShipmentId(),
            savedShipment.getAllocationId(),
            savedShipment.getReservationId(),
            driver.getDriverId(),
            vehicle.getVehicleId(),
            savedShipment.getReceiverId(),
            driver.getFirstName() + " " + driver.getLastName(),
            vehicle.getPlateNumber(),
            savedShipment.getScheduledPickupAt(),
            transportMapper.toAddress(
                savedShipment.getDeliveryStreet(),
                savedShipment.getDeliveryCity(),
                savedShipment.getDeliveryState(),
                savedShipment.getDeliveryCountry(),
                savedShipment.getDeliveryZip()
            )
        );

        outboxService.enqueue(
            KafkaTopics.SHIPMENT_ASSIGNED,
            "SHIPMENT",
            savedShipment.getShipmentId(),
            "ShipmentAssigned",
            payload,
            null
        );

        return transportMapper.toShipmentResponse(savedShipment);
    }

    @Transactional
    public ShipmentResponse pickupShipment(UUID shipmentId, String currentUserId) {
        log.info("Marking shipment: {} as picked up by user: {}", shipmentId, currentUserId);

        Shipment shipment = findShipmentOrThrow(shipmentId);

        if (shipment.getStatus() != ShipmentStatus.ASSIGNED) {
            throw new BusinessRuleException("Shipment must be in ASSIGNED status to be picked up. Current: " + shipment.getStatus());
        }

        verifyDriverOwnership(shipment, currentUserId);

        Instant now = Instant.now();
        shipment.setStatus(ShipmentStatus.PICKED_UP);
        shipment.setPickedUpAt(now);
        Shipment saved = shipmentRepository.save(shipment);

        recordTrackingEvent(shipmentId, "PICKED_UP", "Relief goods picked up from warehouse");

        ShipmentPickedUpPayload payload = new ShipmentPickedUpPayload(
            saved.getShipmentId(),
            saved.getDriverId(),
            saved.getReceiverId(),
            saved.getAllocationId(),
            now
        );

        outboxService.enqueue(
            KafkaTopics.SHIPMENT_PICKED_UP,
            "SHIPMENT",
            saved.getShipmentId(),
            "ShipmentPickedUp",
            payload,
            null
        );

        return transportMapper.toShipmentResponse(saved);
    }

    @Transactional
    public ShipmentResponse startTransit(UUID shipmentId, String currentUserId) {
        log.info("Marking shipment: {} as in transit by user: {}", shipmentId, currentUserId);

        Shipment shipment = findShipmentOrThrow(shipmentId);

        if (shipment.getStatus() != ShipmentStatus.PICKED_UP) {
            throw new BusinessRuleException("Shipment must be in PICKED_UP status to enter IN_TRANSIT. Current: " + shipment.getStatus());
        }

        verifyDriverOwnership(shipment, currentUserId);

        shipment.setStatus(ShipmentStatus.IN_TRANSIT);
        Shipment saved = shipmentRepository.save(shipment);

        recordTrackingEvent(shipmentId, "IN_TRANSIT", "Shipment departed warehouse and is actively en route");

        return transportMapper.toShipmentResponse(saved);
    }

    @Transactional
    public ShipmentResponse deliverShipment(UUID shipmentId, DeliverShipmentRequest request, String currentUserId) {
        log.info("Marking shipment: {} as delivered by user: {}", shipmentId, currentUserId);

        Shipment shipment = findShipmentOrThrow(shipmentId);

        if (shipment.getStatus() != ShipmentStatus.PICKED_UP && shipment.getStatus() != ShipmentStatus.IN_TRANSIT) {
            throw new BusinessRuleException("Shipment must be in PICKED_UP or IN_TRANSIT status to confirm delivery. Current: " + shipment.getStatus());
        }

        verifyDriverOwnership(shipment, currentUserId);

        Instant deliveryTime = request.deliveredAt() != null ? request.deliveredAt() : Instant.now();
        shipment.setStatus(ShipmentStatus.DELIVERED);
        shipment.setDeliveredAt(deliveryTime);
        shipment.setProofOfDeliveryUrl(request.proofOfDeliveryUrl());
        shipment.setRecipientSignature(request.recipientSignature());
        Shipment saved = shipmentRepository.save(shipment);

        // Free the driver
        if (saved.getDriverId() != null) {
            driverRepository.findByDriverIdAndDeletedAtIsNull(saved.getDriverId()).ifPresent(driver -> {
                driver.setAvailable(true);
                driver.setActiveShipmentId(null);
                driverRepository.save(driver);
            });
        }

        recordTrackingEvent(shipmentId, "DELIVERED", "Shipment successfully delivered to recipient");

        ShipmentDeliveredPayload payload = new ShipmentDeliveredPayload(
            saved.getShipmentId(),
            saved.getAllocationId(),
            saved.getReservationId(),
            saved.getReceiverId(),
            saved.getDriverId(),
            deliveryTime,
            saved.getProofOfDeliveryUrl()
        );

        outboxService.enqueue(
            KafkaTopics.SHIPMENT_DELIVERED,
            "SHIPMENT",
            saved.getShipmentId(),
            "ShipmentDelivered",
            payload,
            null
        );

        return transportMapper.toShipmentResponse(saved);
    }

    @Transactional
    public ShipmentResponse failShipment(UUID shipmentId, String reason, String currentUserId) {
        log.warn("Failing shipment: {} due to: {}", shipmentId, reason);

        Shipment shipment = findShipmentOrThrow(shipmentId);

        if (shipment.getStatus() == ShipmentStatus.DELIVERED || shipment.getStatus() == ShipmentStatus.CANCELLED) {
            throw new BusinessRuleException("Cannot fail shipment in terminal status: " + shipment.getStatus());
        }

        verifyDriverOwnership(shipment, currentUserId);

        shipment.setStatus(ShipmentStatus.FAILED);
        shipment.setDisputeReason(reason);
        Shipment saved = shipmentRepository.save(shipment);

        if (saved.getDriverId() != null) {
            driverRepository.findByDriverIdAndDeletedAtIsNull(saved.getDriverId()).ifPresent(driver -> {
                driver.setAvailable(true);
                driver.setActiveShipmentId(null);
                driverRepository.save(driver);
            });
        }

        recordTrackingEvent(shipmentId, "FAILED", "Shipment transit failed: " + reason);

        ShipmentFailedPayload payload = new ShipmentFailedPayload(
            saved.getShipmentId(),
            saved.getAllocationId(),
            saved.getReservationId(),
            saved.getDriverId(),
            saved.getVehicleId(),
            reason,
            Instant.now()
        );

        outboxService.enqueue(
            KafkaTopics.SHIPMENT_FAILED,
            "SHIPMENT",
            saved.getShipmentId(),
            "ShipmentFailed",
            payload,
            null
        );

        return transportMapper.toShipmentResponse(saved);
    }

    @Transactional
    public ShipmentResponse disputeShipment(UUID shipmentId, DisputeShipmentRequest request, String disputedBy) {
        log.info("Disputing shipment: {} by: {} for reason: {}", shipmentId, disputedBy, request.reason());

        Shipment shipment = findShipmentOrThrow(shipmentId);

        if (shipment.getStatus() != ShipmentStatus.ASSIGNED &&
            shipment.getStatus() != ShipmentStatus.PICKED_UP &&
            shipment.getStatus() != ShipmentStatus.IN_TRANSIT &&
            shipment.getStatus() != ShipmentStatus.DELIVERED) {
            throw new BusinessRuleException("Cannot dispute shipment in status: " + shipment.getStatus());
        }

        shipment.setStatus(ShipmentStatus.DISPUTED);
        shipment.setDisputeReason(request.reason());
        shipment.setDisputeEvidenceUrl(request.evidenceUrl());
        Shipment saved = shipmentRepository.save(shipment);

        // Free the driver if still locked
        if (saved.getDriverId() != null) {
            driverRepository.findByDriverIdAndDeletedAtIsNull(saved.getDriverId()).ifPresent(driver -> {
                if (shipmentId.equals(driver.getActiveShipmentId())) {
                    driver.setAvailable(true);
                    driver.setActiveShipmentId(null);
                    driverRepository.save(driver);
                }
            });
        }

        recordTrackingEvent(shipmentId, "DISPUTED", "Delivery disputed by " + disputedBy + ": " + request.reason());

        DeliveryDisputedPayload payload = new DeliveryDisputedPayload(
            saved.getShipmentId(),
            saved.getAllocationId(),
            saved.getReceiverId(),
            saved.getDriverId(),
            request.reason(),
            request.evidenceUrl(),
            disputedBy
        );

        outboxService.enqueue(
            KafkaTopics.DELIVERY_DISPUTED,
            "SHIPMENT",
            saved.getShipmentId(),
            "DeliveryDisputed",
            payload,
            null
        );

        return transportMapper.toShipmentResponse(saved);
    }

    /**
     * Handles vehicle breakdown: marks vehicle inactive, frees affected driver, and attempts
     * automated reassignment of active shipments to an alternative available driver/vehicle.
     */
    @Transactional
    public void handleVehicleBreakdown(UUID vehicleId, String reason) {
        log.warn("Handling vehicle breakdown for vehicleId: {}, reason: {}", vehicleId, reason);

        List<Shipment> activeShipments = shipmentRepository.findAllByVehicleIdAndStatusInAndDeletedAtIsNull(
            vehicleId,
            List.of(ShipmentStatus.ASSIGNED, ShipmentStatus.PICKED_UP, ShipmentStatus.IN_TRANSIT)
        );

        for (Shipment shipment : activeShipments) {
            // Free the driver associated with the broken down vehicle
            if (shipment.getDriverId() != null) {
                driverRepository.findByDriverIdAndDeletedAtIsNull(shipment.getDriverId()).ifPresent(driver -> {
                    driver.setAvailable(true);
                    driver.setActiveShipmentId(null);
                    driverRepository.save(driver);
                });
            }

            // Find alternative available driver with an active vehicle
            Optional<Driver> altDriverOpt = driverRepository.findAllByAvailableTrueAndDeletedAtIsNull().stream()
                .filter(d -> !d.getDriverId().equals(shipment.getDriverId()))
                .filter(d -> vehicleRepository.findFirstByDriverIdAndActiveTrueAndDeletedAtIsNull(d.getDriverId()).isPresent())
                .findFirst();

            if (altDriverOpt.isPresent()) {
                Driver altDriver = altDriverOpt.get();
                Vehicle altVehicle = vehicleRepository.findFirstByDriverIdAndActiveTrueAndDeletedAtIsNull(altDriver.getDriverId()).orElseThrow();

                shipment.setDriverId(altDriver.getDriverId());
                shipment.setVehicleId(altVehicle.getVehicleId());
                shipment.setStatus(ShipmentStatus.ASSIGNED);
                Shipment saved = shipmentRepository.save(shipment);

                altDriver.setAvailable(false);
                altDriver.setActiveShipmentId(saved.getShipmentId());
                driverRepository.save(altDriver);

                String reassignmentDesc = "Reassigned to driver " + altDriver.getFirstName() + " " + altDriver.getLastName()
                    + " (" + altVehicle.getPlateNumber() + ") following breakdown: " + reason;
                recordTrackingEvent(saved.getShipmentId(), "REASSIGNED", reassignmentDesc);

                ShipmentAssignedPayload payload = new ShipmentAssignedPayload(
                    saved.getShipmentId(),
                    saved.getAllocationId(),
                    saved.getReservationId(),
                    altDriver.getDriverId(),
                    altVehicle.getVehicleId(),
                    saved.getReceiverId(),
                    altDriver.getFirstName() + " " + altDriver.getLastName(),
                    altVehicle.getPlateNumber(),
                    saved.getScheduledPickupAt(),
                    transportMapper.toAddress(
                        saved.getDeliveryStreet(),
                        saved.getDeliveryCity(),
                        saved.getDeliveryState(),
                        saved.getDeliveryCountry(),
                        saved.getDeliveryZip()
                    )
                );

                outboxService.enqueue(
                    KafkaTopics.SHIPMENT_ASSIGNED,
                    "SHIPMENT",
                    saved.getShipmentId(),
                    "ShipmentAssigned",
                    payload,
                    null
                );
                log.info("Shipment {} successfully reassigned after breakdown to driver {}", saved.getShipmentId(), altDriver.getDriverId());
            } else {
                // No alternative driver available: revert to CREATED and publish ShipmentFailed event
                shipment.setStatus(ShipmentStatus.CREATED);
                shipment.setDriverId(null);
                shipment.setVehicleId(null);
                Shipment saved = shipmentRepository.save(shipment);

                recordTrackingEvent(saved.getShipmentId(), "BREAKDOWN", "Vehicle breakdown reported: " + reason + ". Awaiting new driver assignment.");

                ShipmentFailedPayload payload = new ShipmentFailedPayload(
                    saved.getShipmentId(),
                    saved.getAllocationId(),
                    saved.getReservationId(),
                    null,
                    vehicleId,
                    "Vehicle breakdown without available replacement: " + reason,
                    Instant.now()
                );

                outboxService.enqueue(
                    KafkaTopics.SHIPMENT_FAILED,
                    "SHIPMENT",
                    saved.getShipmentId(),
                    "ShipmentFailed",
                    payload,
                    null
                );
                log.warn("No replacement vehicle found for shipment {}. Reset status to CREATED.", saved.getShipmentId());
            }
        }
    }

    /**
     * Handles road closure events: correlates affected transit routes and records detour tracking events
     * or triggers reassignment if destination corridor is impassable.
     */
    @Transactional
    public void handleRoadClosure(RoadClosedPayload roadClosed) {
        log.info("Handling road closure event for segment: {} (Road: {})", roadClosed.roadSegmentId(), roadClosed.roadName());

        List<Shipment> activeShipments = shipmentRepository.findAllByStatusInAndDeletedAtIsNull(
            List.of(ShipmentStatus.ASSIGNED, ShipmentStatus.PICKED_UP, ShipmentStatus.IN_TRANSIT)
        );

        // Fetch disaster context if available
        if (roadClosed.disasterId() != null) {
            DisasterClientDto disaster = disasterClient.getDisasterById(roadClosed.disasterId());
            if (disaster != null && disaster.affectedArea() != null) {
                log.info("Enriched road closure with disaster area: {}, radius: {} km",
                    disaster.affectedArea().regionName(), disaster.affectedArea().radiusKm());
            }
        }

        for (Shipment shipment : activeShipments) {
            boolean roadImpeded = false;

            // Address region / city matching
            if (shipment.getDeliveryCity() != null && roadClosed.roadName() != null &&
                (shipment.getDeliveryCity().toLowerCase().contains(roadClosed.roadName().toLowerCase())
                    || roadClosed.reason().toLowerCase().contains(shipment.getDeliveryCity().toLowerCase()))) {
                roadImpeded = true;
            }

            if (roadImpeded) {
                recordTrackingEvent(
                    shipment.getShipmentId(),
                    "ROAD_CLOSED",
                    "Route transit alert: " + roadClosed.roadName() + " closed (" + roadClosed.reason() + "). Detour advised."
                );
                log.warn("Recorded ROAD_CLOSED alert for active shipment {}", shipment.getShipmentId());
            }
        }
    }

    /**
     * Recommends and automatically assigns the best available driver and vehicle based on
     * vehicle capacity, driver availability, and optional disaster proximity.
     */
    @Transactional
    public ShipmentResponse autoAssignShipment(UUID shipmentId, UUID disasterId) {
        log.info("Auto-assigning driver and vehicle for shipment ID: {}, disaster ID: {}", shipmentId, disasterId);

        Shipment shipment = findShipmentOrThrow(shipmentId);
        if (shipment.getStatus() != ShipmentStatus.CREATED) {
            throw new BusinessRuleException("Cannot auto-assign shipment in status: " + shipment.getStatus());
        }

        if (disasterId != null) {
            DisasterClientDto disaster = disasterClient.getDisasterById(disasterId);
            if (disaster != null) {
                log.info("Assessing disaster {} for shipment routing", disaster.name());
            }
        }

        List<Driver> availableDrivers = driverRepository.findAllByAvailableTrueAndDeletedAtIsNull();
        if (availableDrivers.isEmpty()) {
            throw new BusinessRuleException("No drivers are currently available for dispatch");
        }

        // Rank available drivers by largest vehicle carrying capacity
        Driver selectedDriver = null;
        Vehicle selectedVehicle = null;

        for (Driver driver : availableDrivers) {
            Optional<Vehicle> vehicleOpt = vehicleRepository.findFirstByDriverIdAndActiveTrueAndDeletedAtIsNull(driver.getDriverId());
            if (vehicleOpt.isPresent()) {
                selectedDriver = driver;
                selectedVehicle = vehicleOpt.get();
                break;
            }
        }

        if (selectedDriver == null || selectedVehicle == null) {
            throw new BusinessRuleException("No drivers with active vehicles are currently available");
        }

        AssignDriverRequest request = new AssignDriverRequest(selectedDriver.getDriverId(), selectedVehicle.getVehicleId());
        return assignDriver(shipmentId, request);
    }

    public TrackingResponse getTracking(UUID shipmentId) {
        Shipment shipment = findShipmentOrThrow(shipmentId);

        String driverName = null;
        if (shipment.getDriverId() != null) {
            driverName = driverRepository.findByDriverIdAndDeletedAtIsNull(shipment.getDriverId())
                .map(d -> d.getFirstName() + " " + d.getLastName())
                .orElse(null);
        }

        String vehiclePlate = null;
        if (shipment.getVehicleId() != null) {
            vehiclePlate = vehicleRepository.findByVehicleIdAndDeletedAtIsNull(shipment.getVehicleId())
                .map(Vehicle::getPlateNumber)
                .orElse(null);
        }

        List<ShipmentTrackingEvent> events = trackingEventRepository.findAllByShipmentIdOrderByOccurredAtAsc(shipmentId);
        List<TrackingEventDto> dtoList = events.stream().map(transportMapper::toTrackingEventDto).toList();

        return new TrackingResponse(
            shipment.getShipmentId(),
            shipment.getStatus(),
            driverName,
            vehiclePlate,
            shipment.getUpdatedAt(),
            dtoList
        );
    }

    public ShipmentResponse getShipmentById(UUID shipmentId) {
        return transportMapper.toShipmentResponse(findShipmentOrThrow(shipmentId));
    }

    public Page<ShipmentResponse> findShipments(ShipmentStatus status, UUID driverId, Pageable pageable) {
        if (status != null && driverId != null) {
            return shipmentRepository.findAllByDriverIdAndStatusAndDeletedAtIsNull(driverId, status, pageable)
                .map(transportMapper::toShipmentResponse);
        } else if (status != null) {
            return shipmentRepository.findAllByStatusAndDeletedAtIsNull(status, pageable)
                .map(transportMapper::toShipmentResponse);
        } else if (driverId != null) {
            return shipmentRepository.findAllByDriverIdAndDeletedAtIsNull(driverId, pageable)
                .map(transportMapper::toShipmentResponse);
        }
        return shipmentRepository.findAllByDeletedAtIsNull(pageable)
            .map(transportMapper::toShipmentResponse);
    }

    public Shipment findShipmentOrThrow(UUID shipmentId) {
        return shipmentRepository.findByShipmentIdAndDeletedAtIsNull(shipmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Shipment", shipmentId));
    }

    private void recordTrackingEvent(UUID shipmentId, String eventType, String description) {
        ShipmentTrackingEvent event = new ShipmentTrackingEvent();
        event.setShipmentId(shipmentId);
        event.setEventType(eventType);
        event.setDescription(description);
        event.setOccurredAt(Instant.now());
        trackingEventRepository.save(event);
    }

    private void verifyDriverOwnership(Shipment shipment, String currentUserId) {
        if (currentUserId != null && shipment.getDriverId() != null) {
            driverRepository.findByUserIdAndDeletedAtIsNull(currentUserId).ifPresent(driver -> {
                if (!shipment.getDriverId().equals(driver.getDriverId())) {
                    throw new ForbiddenException("Driver user is not authorized to modify shipment " + shipment.getShipmentId());
                }
            });
        }
    }
}
