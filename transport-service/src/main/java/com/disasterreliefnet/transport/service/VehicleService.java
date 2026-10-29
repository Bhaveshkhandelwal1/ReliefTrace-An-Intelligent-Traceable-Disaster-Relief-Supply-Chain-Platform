package com.disasterreliefnet.transport.service;

import com.disasterreliefnet.common.exception.ConflictException;
import com.disasterreliefnet.common.exception.ResourceNotFoundException;
import com.disasterreliefnet.transport.domain.Vehicle;
import com.disasterreliefnet.transport.dto.BreakdownRequest;
import com.disasterreliefnet.transport.dto.CreateVehicleRequest;
import com.disasterreliefnet.transport.dto.VehicleResponse;
import com.disasterreliefnet.transport.mapper.TransportMapper;
import com.disasterreliefnet.transport.repository.VehicleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Service managing vehicles, specifications, driver assignments, availability, and breakdowns.
 */
@Service
@Transactional(readOnly = true)
public class VehicleService {

    private static final Logger log = LoggerFactory.getLogger(VehicleService.class);

    private final VehicleRepository vehicleRepository;
    private final DriverService driverService;
    private final ShipmentService shipmentService;
    private final TransportMapper transportMapper;

    public VehicleService(
        VehicleRepository vehicleRepository,
        DriverService driverService,
        @Lazy ShipmentService shipmentService,
        TransportMapper transportMapper
    ) {
        this.vehicleRepository = vehicleRepository;
        this.driverService = driverService;
        this.shipmentService = shipmentService;
        this.transportMapper = transportMapper;
    }

    @Transactional
    public VehicleResponse registerVehicle(CreateVehicleRequest request) {
        log.info("Registering vehicle plate: {} for driver ID: {}", request.plateNumber(), request.driverId());

        // Validate driver exists and is active
        driverService.findDriverOrThrow(request.driverId());

        if (vehicleRepository.existsByPlateNumberAndDeletedAtIsNull(request.plateNumber())) {
            throw new ConflictException("Vehicle with plate number " + request.plateNumber() + " already exists");
        }

        Vehicle vehicle = transportMapper.toVehicle(request);
        Vehicle saved = vehicleRepository.save(vehicle);
        log.info("Vehicle registered successfully with ID: {}", saved.getVehicleId());
        return transportMapper.toVehicleResponse(saved);
    }

    public VehicleResponse getVehicleById(UUID vehicleId) {
        return transportMapper.toVehicleResponse(findVehicleOrThrow(vehicleId));
    }

    public Page<VehicleResponse> findVehicles(UUID driverId, Pageable pageable) {
        if (driverId != null) {
            return vehicleRepository.findAllByDriverIdAndDeletedAtIsNull(driverId, pageable)
                .map(transportMapper::toVehicleResponse);
        }
        return vehicleRepository.findAllByDeletedAtIsNull(pageable)
            .map(transportMapper::toVehicleResponse);
    }

    @Transactional
    public VehicleResponse deactivateVehicle(UUID vehicleId) {
        log.info("Deactivating vehicle ID: {}", vehicleId);
        Vehicle vehicle = findVehicleOrThrow(vehicleId);
        vehicle.setActive(false);
        Vehicle saved = vehicleRepository.save(vehicle);
        return transportMapper.toVehicleResponse(saved);
    }

    /**
     * Reports a vehicle breakdown, deactivating the vehicle and triggering automatic reassignment
     * of any in-flight shipments to other available drivers.
     */
    @Transactional
    public VehicleResponse reportBreakdown(UUID vehicleId, BreakdownRequest request) {
        log.warn("Reporting breakdown for vehicle ID: {}, reason: {}", vehicleId, request.reason());
        Vehicle vehicle = findVehicleOrThrow(vehicleId);
        vehicle.setActive(false);
        Vehicle saved = vehicleRepository.save(vehicle);

        shipmentService.handleVehicleBreakdown(vehicleId, request.reason());
        return transportMapper.toVehicleResponse(saved);
    }

    @Transactional
    public void deleteVehicle(UUID vehicleId) {
        log.info("Soft-deleting vehicle ID: {}", vehicleId);
        Vehicle vehicle = findVehicleOrThrow(vehicleId);
        vehicle.setDeletedAt(Instant.now());
        vehicle.setActive(false);
        vehicleRepository.save(vehicle);
    }

    public Vehicle findVehicleOrThrow(UUID vehicleId) {
        return vehicleRepository.findByVehicleIdAndDeletedAtIsNull(vehicleId)
            .orElseThrow(() -> new ResourceNotFoundException("Vehicle", vehicleId));
    }
}
