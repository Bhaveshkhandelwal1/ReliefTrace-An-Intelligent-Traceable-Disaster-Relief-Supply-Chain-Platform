package com.disasterreliefnet.transport.service;

import com.disasterreliefnet.common.dto.DeliveryAddressDto;
import com.disasterreliefnet.common.event.KafkaTopics;
import com.disasterreliefnet.common.event.payload.StockReservedPayload;
import com.disasterreliefnet.common.exception.BusinessRuleException;
import com.disasterreliefnet.common.exception.ConflictException;
import com.disasterreliefnet.common.exception.ResourceNotFoundException;
import com.disasterreliefnet.common.outbox.OutboxService;
import com.disasterreliefnet.transport.client.DisasterClient;
import com.disasterreliefnet.transport.domain.*;
import com.disasterreliefnet.transport.dto.*;
import com.disasterreliefnet.transport.mapper.TransportMapper;
import com.disasterreliefnet.transport.mapper.TransportMapperImpl;
import com.disasterreliefnet.transport.repository.DriverRepository;
import com.disasterreliefnet.transport.repository.ShipmentRepository;
import com.disasterreliefnet.transport.repository.ShipmentTrackingEventRepository;
import com.disasterreliefnet.transport.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShipmentServiceTest {

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private ShipmentTrackingEventRepository trackingEventRepository;

    @Mock
    private OutboxService outboxService;

    @Mock
    private DisasterClient disasterClient;

    private final TransportMapper transportMapper = new TransportMapperImpl();
    private ShipmentService shipmentService;

    @BeforeEach
    void setUp() {
        shipmentService = new ShipmentService(
            shipmentRepository,
            driverRepository,
            vehicleRepository,
            trackingEventRepository,
            outboxService,
            transportMapper,
            disasterClient
        );
    }

    @Test
    @DisplayName("Should create shipment manually")
    void createShipment_Success() {
        UUID allocationId = UUID.randomUUID();
        CreateShipmentRequest request = new CreateShipmentRequest(
            allocationId,
            UUID.randomUUID(),
            new DeliveryAddressDto("100 Depot Way", "Austin", "TX", "US", "78701"),
            new DeliveryAddressDto("500 Shelter Rd", "Houston", "TX", "US", "77002"),
            UUID.randomUUID(),
            Instant.now()
        );

        when(shipmentRepository.findByAllocationIdAndDeletedAtIsNull(allocationId)).thenReturn(Optional.empty());
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(i -> {
            Shipment s = i.getArgument(0);
            s.setShipmentId(UUID.randomUUID());
            return s;
        });

        ShipmentResponse response = shipmentService.createShipment(request);

        assertNotNull(response);
        assertEquals(allocationId, response.allocationId());
        assertEquals(ShipmentStatus.CREATED, response.status());
        verify(trackingEventRepository).save(any(ShipmentTrackingEvent.class));
    }

    @Test
    @DisplayName("Should throw ConflictException when creating duplicate shipment for allocation")
    void createShipment_DuplicateAllocation() {
        UUID allocationId = UUID.randomUUID();
        CreateShipmentRequest request = new CreateShipmentRequest(
            allocationId,
            UUID.randomUUID(),
            new DeliveryAddressDto("100 Depot Way", "Austin", "TX", "US", "78701"),
            new DeliveryAddressDto("500 Shelter Rd", "Houston", "TX", "US", "77002"),
            UUID.randomUUID(),
            Instant.now()
        );

        when(shipmentRepository.findByAllocationIdAndDeletedAtIsNull(allocationId)).thenReturn(Optional.of(new Shipment()));

        assertThrows(ConflictException.class, () -> shipmentService.createShipment(request));
        verify(shipmentRepository, never()).save(any(Shipment.class));
    }

    @Test
    @DisplayName("Should create shipment from stock.reserved event payload")
    void createShipmentFromStockReserved_Success() {
        UUID allocationId = UUID.randomUUID();
        StockReservedPayload payload = new StockReservedPayload(
            UUID.randomUUID(),
            allocationId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            BigDecimal.valueOf(100),
            "BOXES",
            new DeliveryAddressDto("Depot A", "Miami", "FL", "US", "33101"),
            new DeliveryAddressDto("Camp B", "Key West", "FL", "US", "33040")
        );

        when(shipmentRepository.findByAllocationIdAndDeletedAtIsNull(allocationId)).thenReturn(Optional.empty());
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(i -> {
            Shipment s = i.getArgument(0);
            s.setShipmentId(UUID.randomUUID());
            return s;
        });

        shipmentService.createShipmentFromStockReserved(payload);

        verify(shipmentRepository).save(any(Shipment.class));
        verify(trackingEventRepository).save(any(ShipmentTrackingEvent.class));
    }

    @Test
    @DisplayName("Should assign driver and vehicle to shipment and emit outbox event")
    void assignDriver_Success() {
        UUID shipmentId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();

        Shipment shipment = new Shipment();
        shipment.setShipmentId(shipmentId);
        shipment.setAllocationId(UUID.randomUUID());
        shipment.setReservationId(UUID.randomUUID());
        shipment.setReceiverId(UUID.randomUUID());
        shipment.setStatus(ShipmentStatus.CREATED);

        Driver driver = new Driver();
        driver.setDriverId(driverId);
        driver.setFirstName("Bob");
        driver.setLastName("Driver");
        driver.setAvailable(true);

        Vehicle vehicle = new Vehicle();
        vehicle.setVehicleId(vehicleId);
        vehicle.setDriverId(driverId);
        vehicle.setPlateNumber("TX-1234");
        vehicle.setActive(true);

        when(shipmentRepository.findByShipmentIdAndDeletedAtIsNull(shipmentId)).thenReturn(Optional.of(shipment));
        when(driverRepository.findByDriverIdAndDeletedAtIsNull(driverId)).thenReturn(Optional.of(driver));
        when(vehicleRepository.findByVehicleIdAndDeletedAtIsNull(vehicleId)).thenReturn(Optional.of(vehicle));
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(i -> i.getArgument(0));

        AssignDriverRequest request = new AssignDriverRequest(driverId, vehicleId);
        ShipmentResponse response = shipmentService.assignDriver(shipmentId, request);

        assertNotNull(response);
        assertEquals(ShipmentStatus.ASSIGNED, response.status());
        assertEquals(driverId, shipment.getDriverId());
        assertEquals(vehicleId, shipment.getVehicleId());
        assertFalse(driver.isAvailable());
        assertEquals(shipmentId, driver.getActiveShipmentId());

        verify(driverRepository).save(driver);
        verify(trackingEventRepository).save(any(ShipmentTrackingEvent.class));
        verify(outboxService).enqueue(
            eq(KafkaTopics.SHIPMENT_ASSIGNED),
            eq("SHIPMENT"),
            eq(shipmentId),
            eq("ShipmentAssigned"),
            any(),
            isNull()
        );
    }

    @Test
    @DisplayName("Should reject assigning driver when vehicle does not belong to driver")
    void assignDriver_VehicleMismatch() {
        UUID shipmentId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();

        Shipment shipment = new Shipment();
        shipment.setShipmentId(shipmentId);
        shipment.setStatus(ShipmentStatus.CREATED);

        Driver driver = new Driver();
        driver.setDriverId(driverId);
        driver.setAvailable(true);

        Vehicle vehicle = new Vehicle();
        vehicle.setVehicleId(vehicleId);
        vehicle.setDriverId(UUID.randomUUID()); // Different driver
        vehicle.setActive(true);

        when(shipmentRepository.findByShipmentIdAndDeletedAtIsNull(shipmentId)).thenReturn(Optional.of(shipment));
        when(driverRepository.findByDriverIdAndDeletedAtIsNull(driverId)).thenReturn(Optional.of(driver));
        when(vehicleRepository.findByVehicleIdAndDeletedAtIsNull(vehicleId)).thenReturn(Optional.of(vehicle));

        AssignDriverRequest request = new AssignDriverRequest(driverId, vehicleId);
        assertThrows(BusinessRuleException.class, () -> shipmentService.assignDriver(shipmentId, request));
    }

    @Test
    @DisplayName("Should mark shipment as picked up")
    void pickupShipment_Success() {
        UUID shipmentId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();

        Shipment shipment = new Shipment();
        shipment.setShipmentId(shipmentId);
        shipment.setAllocationId(UUID.randomUUID());
        shipment.setReceiverId(UUID.randomUUID());
        shipment.setDriverId(driverId);
        shipment.setStatus(ShipmentStatus.ASSIGNED);

        when(shipmentRepository.findByShipmentIdAndDeletedAtIsNull(shipmentId)).thenReturn(Optional.of(shipment));
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(i -> i.getArgument(0));

        ShipmentResponse response = shipmentService.pickupShipment(shipmentId, null);

        assertNotNull(response);
        assertEquals(ShipmentStatus.PICKED_UP, response.status());
        assertNotNull(shipment.getPickedUpAt());

        verify(outboxService).enqueue(
            eq(KafkaTopics.SHIPMENT_PICKED_UP),
            eq("SHIPMENT"),
            eq(shipmentId),
            eq("ShipmentPickedUp"),
            any(),
            isNull()
        );
    }

    @Test
    @DisplayName("Should confirm shipment delivery and free the driver")
    void deliverShipment_Success() {
        UUID shipmentId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();

        Shipment shipment = new Shipment();
        shipment.setShipmentId(shipmentId);
        shipment.setAllocationId(UUID.randomUUID());
        shipment.setReservationId(UUID.randomUUID());
        shipment.setReceiverId(UUID.randomUUID());
        shipment.setDriverId(driverId);
        shipment.setStatus(ShipmentStatus.PICKED_UP);

        Driver driver = new Driver();
        driver.setDriverId(driverId);
        driver.setAvailable(false);
        driver.setActiveShipmentId(shipmentId);

        when(shipmentRepository.findByShipmentIdAndDeletedAtIsNull(shipmentId)).thenReturn(Optional.of(shipment));
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(i -> i.getArgument(0));
        when(driverRepository.findByDriverIdAndDeletedAtIsNull(driverId)).thenReturn(Optional.of(driver));

        DeliverShipmentRequest request = new DeliverShipmentRequest(
            "https://s3.amazonaws.com/relief/pod.jpg",
            "Recipient Signature",
            Instant.now()
        );

        ShipmentResponse response = shipmentService.deliverShipment(shipmentId, request, null);

        assertNotNull(response);
        assertEquals(ShipmentStatus.DELIVERED, response.status());
        assertTrue(driver.isAvailable());
        assertNull(driver.getActiveShipmentId());

        verify(driverRepository).save(driver);
        verify(outboxService).enqueue(
            eq(KafkaTopics.SHIPMENT_DELIVERED),
            eq("SHIPMENT"),
            eq(shipmentId),
            eq("ShipmentDelivered"),
            any(),
            isNull()
        );
    }

    @Test
    @DisplayName("Should dispute shipment delivery and emit outbox event")
    void disputeShipment_Success() {
        UUID shipmentId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();

        Shipment shipment = new Shipment();
        shipment.setShipmentId(shipmentId);
        shipment.setAllocationId(UUID.randomUUID());
        shipment.setReceiverId(UUID.randomUUID());
        shipment.setDriverId(driverId);
        shipment.setStatus(ShipmentStatus.PICKED_UP);

        Driver driver = new Driver();
        driver.setDriverId(driverId);
        driver.setAvailable(false);
        driver.setActiveShipmentId(shipmentId);

        when(shipmentRepository.findByShipmentIdAndDeletedAtIsNull(shipmentId)).thenReturn(Optional.of(shipment));
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(i -> i.getArgument(0));
        when(driverRepository.findByDriverIdAndDeletedAtIsNull(driverId)).thenReturn(Optional.of(driver));

        DisputeShipmentRequest request = new DisputeShipmentRequest("Goods damaged during transit", "https://s3/evidence.jpg");
        ShipmentResponse response = shipmentService.disputeShipment(shipmentId, request, "RECEIVER");

        assertNotNull(response);
        assertEquals(ShipmentStatus.DISPUTED, response.status());
        assertEquals("Goods damaged during transit", shipment.getDisputeReason());

        verify(outboxService).enqueue(
            eq(KafkaTopics.DELIVERY_DISPUTED),
            eq("SHIPMENT"),
            eq(shipmentId),
            eq("DeliveryDisputed"),
            any(),
            isNull()
        );
    }

    @Test
    @DisplayName("Should return full tracking history")
    void getTracking_Success() {
        UUID shipmentId = UUID.randomUUID();
        Shipment shipment = new Shipment();
        shipment.setShipmentId(shipmentId);
        shipment.setStatus(ShipmentStatus.PICKED_UP);
        shipment.setUpdatedAt(Instant.now());

        ShipmentTrackingEvent event1 = new ShipmentTrackingEvent();
        event1.setShipmentId(shipmentId);
        event1.setEventType("CREATED");
        event1.setOccurredAt(Instant.now());
        event1.setDescription("Created");

        when(shipmentRepository.findByShipmentIdAndDeletedAtIsNull(shipmentId)).thenReturn(Optional.of(shipment));
        when(trackingEventRepository.findAllByShipmentIdOrderByOccurredAtAsc(shipmentId)).thenReturn(List.of(event1));

        TrackingResponse tracking = shipmentService.getTracking(shipmentId);

        assertNotNull(tracking);
        assertEquals(shipmentId, tracking.shipmentId());
        assertEquals(ShipmentStatus.PICKED_UP, tracking.status());
        assertEquals(1, tracking.events().size());
    }
}
