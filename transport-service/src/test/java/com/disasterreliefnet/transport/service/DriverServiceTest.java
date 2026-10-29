package com.disasterreliefnet.transport.service;

import com.disasterreliefnet.common.exception.BusinessRuleException;
import com.disasterreliefnet.common.exception.ConflictException;
import com.disasterreliefnet.common.exception.ResourceNotFoundException;
import com.disasterreliefnet.transport.domain.Driver;
import com.disasterreliefnet.transport.dto.CreateDriverRequest;
import com.disasterreliefnet.transport.dto.DriverResponse;
import com.disasterreliefnet.transport.dto.UpdateDriverRequest;
import com.disasterreliefnet.transport.mapper.TransportMapper;
import com.disasterreliefnet.transport.mapper.TransportMapperImpl;
import com.disasterreliefnet.transport.repository.DriverRepository;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DriverServiceTest {

    @Mock
    private DriverRepository driverRepository;

    private final TransportMapper transportMapper = new TransportMapperImpl();
    private DriverService driverService;

    @BeforeEach
    void setUp() {
        driverService = new DriverService(driverRepository, transportMapper);
    }

    @Test
    @DisplayName("Should successfully register a new driver")
    void registerDriver_Success() {
        CreateDriverRequest request = new CreateDriverRequest(
            "user-123",
            "John",
            "Doe",
            "john.doe@transport.org",
            "+1234567890",
            "DL-998877"
        );

        when(driverRepository.existsByEmailAndDeletedAtIsNull(request.email())).thenReturn(false);
        when(driverRepository.existsByUserIdAndDeletedAtIsNull(request.userId())).thenReturn(false);
        when(driverRepository.save(any(Driver.class))).thenAnswer(invocation -> {
            Driver d = invocation.getArgument(0);
            d.setDriverId(UUID.randomUUID());
            return d;
        });

        DriverResponse response = driverService.registerDriver(request);

        assertNotNull(response);
        assertEquals("John", response.firstName());
        assertEquals("Doe", response.lastName());
        assertEquals("john.doe@transport.org", response.email());
        assertTrue(response.available());
        verify(driverRepository, times(1)).save(any(Driver.class));
    }

    @Test
    @DisplayName("Should throw ConflictException when email is already in use")
    void registerDriver_DuplicateEmail_ThrowsConflict() {
        CreateDriverRequest request = new CreateDriverRequest(
            "user-123",
            "John",
            "Doe",
            "john.doe@transport.org",
            "+1234567890",
            "DL-998877"
        );

        when(driverRepository.existsByEmailAndDeletedAtIsNull(request.email())).thenReturn(true);

        assertThrows(ConflictException.class, () -> driverService.registerDriver(request));
        verify(driverRepository, never()).save(any(Driver.class));
    }

    @Test
    @DisplayName("Should throw ConflictException when userId is already in use")
    void registerDriver_DuplicateUserId_ThrowsConflict() {
        CreateDriverRequest request = new CreateDriverRequest(
            "user-123",
            "John",
            "Doe",
            "john.doe@transport.org",
            "+1234567890",
            "DL-998877"
        );

        when(driverRepository.existsByEmailAndDeletedAtIsNull(request.email())).thenReturn(false);
        when(driverRepository.existsByUserIdAndDeletedAtIsNull(request.userId())).thenReturn(true);

        assertThrows(ConflictException.class, () -> driverService.registerDriver(request));
        verify(driverRepository, never()).save(any(Driver.class));
    }

    @Test
    @DisplayName("Should retrieve driver by ID")
    void getDriverById_Success() {
        UUID driverId = UUID.randomUUID();
        Driver driver = new Driver();
        driver.setDriverId(driverId);
        driver.setFirstName("Alice");
        driver.setLastName("Smith");
        driver.setEmail("alice@relief.org");

        when(driverRepository.findByDriverIdAndDeletedAtIsNull(driverId)).thenReturn(Optional.of(driver));

        DriverResponse response = driverService.getDriverById(driverId);

        assertNotNull(response);
        assertEquals(driverId, response.driverId());
        assertEquals("Alice", response.firstName());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when driver not found")
    void getDriverById_NotFound_ThrowsException() {
        UUID driverId = UUID.randomUUID();
        when(driverRepository.findByDriverIdAndDeletedAtIsNull(driverId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> driverService.getDriverById(driverId));
    }

    @Test
    @DisplayName("Should filter available drivers with pagination")
    void findDrivers_AvailableFilter() {
        Driver driver = new Driver();
        driver.setDriverId(UUID.randomUUID());
        driver.setAvailable(true);

        Pageable pageable = PageRequest.of(0, 10);
        when(driverRepository.findAllByAvailableAndDeletedAtIsNull(true, pageable))
            .thenReturn(new PageImpl<>(List.of(driver), pageable, 1));

        Page<DriverResponse> result = driverService.findDrivers(true, pageable);

        assertEquals(1, result.getTotalElements());
        verify(driverRepository).findAllByAvailableAndDeletedAtIsNull(true, pageable);
    }

    @Test
    @DisplayName("Should update driver mutable details")
    void updateDriver_Success() {
        UUID driverId = UUID.randomUUID();
        Driver driver = new Driver();
        driver.setDriverId(driverId);
        driver.setPhone("+1111111111");
        driver.setAvailable(true);

        UpdateDriverRequest updateRequest = new UpdateDriverRequest("+9999999999", "DL-NEW", false);

        when(driverRepository.findByDriverIdAndDeletedAtIsNull(driverId)).thenReturn(Optional.of(driver));
        when(driverRepository.save(any(Driver.class))).thenAnswer(i -> i.getArgument(0));

        DriverResponse response = driverService.updateDriver(driverId, updateRequest);

        assertNotNull(response);
        assertEquals("+9999999999", response.phone());
        assertEquals("DL-NEW", response.licenseNumber());
        assertFalse(response.available());
    }

    @Test
    @DisplayName("Should reject setting driver available while active shipment in progress")
    void updateDriver_ActiveShipmentConflict() {
        UUID driverId = UUID.randomUUID();
        Driver driver = new Driver();
        driver.setDriverId(driverId);
        driver.setActiveShipmentId(UUID.randomUUID());
        driver.setAvailable(false);

        UpdateDriverRequest updateRequest = new UpdateDriverRequest(null, null, true);
        when(driverRepository.findByDriverIdAndDeletedAtIsNull(driverId)).thenReturn(Optional.of(driver));

        assertThrows(BusinessRuleException.class, () -> driverService.updateDriver(driverId, updateRequest));
    }

    @Test
    @DisplayName("Should soft delete driver if no active shipment")
    void deleteDriver_Success() {
        UUID driverId = UUID.randomUUID();
        Driver driver = new Driver();
        driver.setDriverId(driverId);
        driver.setAvailable(true);

        when(driverRepository.findByDriverIdAndDeletedAtIsNull(driverId)).thenReturn(Optional.of(driver));
        when(driverRepository.save(any(Driver.class))).thenAnswer(i -> i.getArgument(0));

        driverService.deleteDriver(driverId);

        assertNotNull(driver.getDeletedAt());
        assertFalse(driver.isAvailable());
        verify(driverRepository).save(driver);
    }

    @Test
    @DisplayName("Should throw BusinessRuleException on deleting driver with active shipment")
    void deleteDriver_ActiveShipment_ThrowsException() {
        UUID driverId = UUID.randomUUID();
        Driver driver = new Driver();
        driver.setDriverId(driverId);
        driver.setActiveShipmentId(UUID.randomUUID());

        when(driverRepository.findByDriverIdAndDeletedAtIsNull(driverId)).thenReturn(Optional.of(driver));

        assertThrows(BusinessRuleException.class, () -> driverService.deleteDriver(driverId));
        verify(driverRepository, never()).save(any(Driver.class));
    }
}
