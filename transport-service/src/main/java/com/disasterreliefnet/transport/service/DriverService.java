package com.disasterreliefnet.transport.service;

import com.disasterreliefnet.common.exception.BusinessRuleException;
import com.disasterreliefnet.common.exception.ConflictException;
import com.disasterreliefnet.common.exception.ResourceNotFoundException;
import com.disasterreliefnet.transport.domain.Driver;
import com.disasterreliefnet.transport.dto.CreateDriverRequest;
import com.disasterreliefnet.transport.dto.DriverResponse;
import com.disasterreliefnet.transport.dto.UpdateDriverRequest;
import com.disasterreliefnet.transport.mapper.TransportMapper;
import com.disasterreliefnet.transport.repository.DriverRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Service managing driver profiles, availability status, and lifecycle.
 */
@Service
@Transactional(readOnly = true)
public class DriverService {

    private static final Logger log = LoggerFactory.getLogger(DriverService.class);

    private final DriverRepository driverRepository;
    private final TransportMapper transportMapper;

    public DriverService(DriverRepository driverRepository, TransportMapper transportMapper) {
        this.driverRepository = driverRepository;
        this.transportMapper = transportMapper;
    }

    @Transactional
    public DriverResponse registerDriver(CreateDriverRequest request) {
        log.info("Registering new driver for user ID: {}, email: {}", request.userId(), request.email());

        if (driverRepository.existsByEmailAndDeletedAtIsNull(request.email())) {
            throw new ConflictException("Driver with email " + request.email() + " already exists");
        }
        if (driverRepository.existsByUserIdAndDeletedAtIsNull(request.userId())) {
            throw new ConflictException("Driver with userId " + request.userId() + " already exists");
        }

        Driver driver = transportMapper.toDriver(request);
        Driver saved = driverRepository.save(driver);
        log.info("Driver registered successfully with ID: {}", saved.getDriverId());
        return transportMapper.toDriverResponse(saved);
    }

    public DriverResponse getDriverById(UUID driverId) {
        return transportMapper.toDriverResponse(findDriverOrThrow(driverId));
    }

    public DriverResponse getDriverByUserId(String userId) {
        Driver driver = driverRepository.findByUserIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Driver", userId));
        return transportMapper.toDriverResponse(driver);
    }

    public Page<DriverResponse> findDrivers(Boolean available, Pageable pageable) {
        if (available != null) {
            return driverRepository.findAllByAvailableAndDeletedAtIsNull(available, pageable)
                .map(transportMapper::toDriverResponse);
        }
        return driverRepository.findAllByDeletedAtIsNull(pageable)
            .map(transportMapper::toDriverResponse);
    }

    @Transactional
    public DriverResponse updateDriver(UUID driverId, UpdateDriverRequest request) {
        log.info("Updating driver ID: {}", driverId);
        Driver driver = findDriverOrThrow(driverId);

        if (request.phone() != null && !request.phone().isBlank()) {
            driver.setPhone(request.phone());
        }
        if (request.licenseNumber() != null && !request.licenseNumber().isBlank()) {
            driver.setLicenseNumber(request.licenseNumber());
        }
        if (request.available() != null) {
            // Cannot manually set available to true if driver has an active shipment
            if (request.available() && driver.getActiveShipmentId() != null) {
                throw new BusinessRuleException("Cannot set driver available while active shipment " + driver.getActiveShipmentId() + " is in progress");
            }
            driver.setAvailable(request.available());
        }

        Driver saved = driverRepository.save(driver);
        return transportMapper.toDriverResponse(saved);
    }

    @Transactional
    public void deleteDriver(UUID driverId) {
        log.info("Soft-deleting driver ID: {}", driverId);
        Driver driver = findDriverOrThrow(driverId);

        if (driver.getActiveShipmentId() != null) {
            throw new BusinessRuleException("Cannot delete driver while an active shipment is in progress");
        }

        driver.setDeletedAt(Instant.now());
        driver.setAvailable(false);
        driverRepository.save(driver);
        log.info("Driver {} marked as deleted", driverId);
    }

    public Driver findDriverOrThrow(UUID driverId) {
        return driverRepository.findByDriverIdAndDeletedAtIsNull(driverId)
            .orElseThrow(() -> new ResourceNotFoundException("Driver", driverId));
    }
}
