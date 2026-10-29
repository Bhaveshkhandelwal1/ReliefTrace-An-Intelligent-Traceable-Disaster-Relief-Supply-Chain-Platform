package com.disasterreliefnet.inventory.service;

import com.disasterreliefnet.common.exception.BusinessRuleException;
import com.disasterreliefnet.common.exception.ResourceNotFoundException;
import com.disasterreliefnet.inventory.domain.Warehouse;
import com.disasterreliefnet.inventory.dto.CreateWarehouseRequest;
import com.disasterreliefnet.inventory.dto.UpdateWarehouseRequest;
import com.disasterreliefnet.inventory.dto.WarehouseResponse;
import com.disasterreliefnet.inventory.mapper.WarehouseMapper;
import com.disasterreliefnet.inventory.repository.WarehouseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Service managing warehouse facility lifecycle, configuration, and capacity.
 */
@Service
public class WarehouseService {

    private static final Logger log = LoggerFactory.getLogger(WarehouseService.class);

    private final WarehouseRepository warehouseRepository;
    private final WarehouseMapper warehouseMapper;

    public WarehouseService(WarehouseRepository warehouseRepository, WarehouseMapper warehouseMapper) {
        this.warehouseRepository = warehouseRepository;
        this.warehouseMapper = warehouseMapper;
    }

    @Transactional
    public WarehouseResponse createWarehouse(CreateWarehouseRequest request) {
        if (warehouseRepository.existsByNameIgnoreCaseAndDeletedAtIsNull(request.name())) {
            throw new BusinessRuleException("Warehouse with name '" + request.name() + "' already exists");
        }

        Warehouse warehouse = warehouseMapper.toEntity(request);
        Warehouse saved = warehouseRepository.save(warehouse);
        log.info("Created new warehouse facility: id={}, name={}", saved.getWarehouseId(), saved.getName());
        return warehouseMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public WarehouseResponse getWarehouse(UUID warehouseId) {
        Warehouse warehouse = findWarehouseOrThrow(warehouseId);
        return warehouseMapper.toResponse(warehouse);
    }

    @Transactional(readOnly = true)
    public Page<WarehouseResponse> getWarehouses(Pageable pageable) {
        return warehouseRepository.findByDeletedAtIsNull(pageable)
            .map(warehouseMapper::toResponse);
    }

    @Transactional
    public WarehouseResponse updateWarehouse(UUID warehouseId, UpdateWarehouseRequest request) {
        Warehouse warehouse = findWarehouseOrThrow(warehouseId);

        if (request.name() != null && !request.name().equalsIgnoreCase(warehouse.getName())
            && warehouseRepository.existsByNameIgnoreCaseAndDeletedAtIsNull(request.name())) {
            throw new BusinessRuleException("Another warehouse with name '" + request.name() + "' already exists");
        }

        warehouseMapper.updateEntityFromDto(request, warehouse);
        Warehouse updated = warehouseRepository.save(warehouse);
        log.info("Updated warehouse facility: id={}, name={}", updated.getWarehouseId(), updated.getName());
        return warehouseMapper.toResponse(updated);
    }

    @Transactional
    public void deactivateWarehouse(UUID warehouseId) {
        Warehouse warehouse = findWarehouseOrThrow(warehouseId);
        warehouse.setActive(false);
        warehouse.setDeletedAt(Instant.now());
        warehouseRepository.save(warehouse);
        log.info("Deactivated warehouse facility: id={}", warehouseId);
    }

    public Warehouse findWarehouseOrThrow(UUID warehouseId) {
        return warehouseRepository.findByWarehouseIdAndDeletedAtIsNull(warehouseId)
            .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + warehouseId));
    }
}
