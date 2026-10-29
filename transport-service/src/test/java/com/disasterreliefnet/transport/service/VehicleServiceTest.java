package com.disasterreliefnet.transport.service;

import com.disasterreliefnet.common.exception.ConflictException;
import com.disasterreliefnet.common.exception.ResourceNotFoundException;
import com.disasterreliefnet.transport.domain.Driver;
import com.disasterreliefnet.transport.domain.Vehicle;
import com.disasterreliefnet.transport.domain.VehicleType;
import com.disasterreliefnet.transport.dto.CreateVehicleRequest;
import com.disasterreliefnet.transport.dto.VehicleResponse;
import com.disasterreliefnet.transport.mapper.TransportMapper;
import com.disasterreliefnet.transport.mapper.TransportMapperImpl;
import com.disasterreliefnet.transport.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private DriverService driverService;

    @Mock
    private ShipmentService shipmentService;

    private final TransportMapper transportMapper = new TransportMapperImpl();
    private VehicleService vehicleService;

    @BeforeEach
    void setUp() {
        vehicleService = new VehicleService(vehicleRepository, driverService, shipmentService, transportMapper);
    }

    @Test
    @DisplayName("Should successfully register a new vehicle")
    void registerVehicle_Success() {
        UUID driverId = UUID.randomUUID();
        CreateVehicleRequest request = new CreateVehicleRequest(
            driverId,
            "XYZ-9988",
            VehicleType.TRUCK,
            BigDecimal.valueOf(5000),
            BigDecimal.valueOf(25)
        );

        Driver driver = new Driver();
        driver.setDriverId(driverId);

        when(driverService.findDriverOrThrow(driverId)).thenReturn(driver);
        when(vehicleRepository.existsByPlateNumberAndDeletedAtIsNull(request.plateNumber())).thenReturn(false);
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(i -> {
            Vehicle v = i.getArgument(0);
            v.setVehicleId(UUID.randomUUID());
            return v;
        });

        VehicleResponse response = vehicleService.registerVehicle(request);

        assertNotNull(response);
        assertEquals("XYZ-9988", response.plateNumber());
        assertEquals(VehicleType.TRUCK, response.type());
        assertTrue(response.active());
        verify(vehicleRepository).save(any(Vehicle.class));
    }

    @Test
    @DisplayName("Should throw ConflictException when plate number is duplicated")
    void registerVehicle_DuplicatePlate() {
        UUID driverId = UUID.randomUUID();
        CreateVehicleRequest request = new CreateVehicleRequest(
            driverId,
            "XYZ-9988",
            VehicleType.VAN,
            BigDecimal.valueOf(1500),
            BigDecimal.valueOf(10)
        );

        when(driverService.findDriverOrThrow(driverId)).thenReturn(new Driver());
        when(vehicleRepository.existsByPlateNumberAndDeletedAtIsNull(request.plateNumber())).thenReturn(true);

        assertThrows(ConflictException.class, () -> vehicleService.registerVehicle(request));
        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    @Test
    @DisplayName("Should retrieve vehicle by ID")
    void getVehicleById_Success() {
        UUID vehicleId = UUID.randomUUID();
        Vehicle vehicle = new Vehicle();
        vehicle.setVehicleId(vehicleId);
        vehicle.setPlateNumber("ABC-1234");
        vehicle.setType(VehicleType.MOTORCYCLE);

        when(vehicleRepository.findByVehicleIdAndDeletedAtIsNull(vehicleId)).thenReturn(Optional.of(vehicle));

        VehicleResponse response = vehicleService.getVehicleById(vehicleId);

        assertNotNull(response);
        assertEquals(vehicleId, response.vehicleId());
        assertEquals("ABC-1234", response.plateNumber());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when vehicle not found")
    void getVehicleById_NotFound() {
        UUID vehicleId = UUID.randomUUID();
        when(vehicleRepository.findByVehicleIdAndDeletedAtIsNull(vehicleId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> vehicleService.getVehicleById(vehicleId));
    }

    @Test
    @DisplayName("Should find vehicles filtered by driver ID")
    void findVehicles_ByDriver() {
        UUID driverId = UUID.randomUUID();
        Vehicle vehicle = new Vehicle();
        vehicle.setVehicleId(UUID.randomUUID());
        vehicle.setDriverId(driverId);

        Pageable pageable = PageRequest.of(0, 10);
        when(vehicleRepository.findAllByDriverIdAndDeletedAtIsNull(driverId, pageable))
            .thenReturn(new PageImpl<>(List.of(vehicle), pageable, 1));

        Page<VehicleResponse> page = vehicleService.findVehicles(driverId, pageable);

        assertEquals(1, page.getTotalElements());
        verify(vehicleRepository).findAllByDriverIdAndDeletedAtIsNull(driverId, pageable);
    }

    @Test
    @DisplayName("Should deactivate vehicle successfully")
    void deactivateVehicle_Success() {
        UUID vehicleId = UUID.randomUUID();
        Vehicle vehicle = new Vehicle();
        vehicle.setVehicleId(vehicleId);
        vehicle.setActive(true);

        when(vehicleRepository.findByVehicleIdAndDeletedAtIsNull(vehicleId)).thenReturn(Optional.of(vehicle));
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(i -> i.getArgument(0));

        VehicleResponse response = vehicleService.deactivateVehicle(vehicleId);

        assertNotNull(response);
        assertFalse(response.active());
        assertFalse(vehicle.isActive());
    }
}
