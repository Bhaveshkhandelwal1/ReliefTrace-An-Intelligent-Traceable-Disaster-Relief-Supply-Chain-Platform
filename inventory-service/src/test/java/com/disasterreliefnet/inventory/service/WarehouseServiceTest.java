package com.disasterreliefnet.inventory.service;

import com.disasterreliefnet.common.exception.BusinessRuleException;
import com.disasterreliefnet.common.exception.ResourceNotFoundException;
import com.disasterreliefnet.inventory.domain.Warehouse;
import com.disasterreliefnet.inventory.dto.AddressDto;
import com.disasterreliefnet.inventory.dto.CreateWarehouseRequest;
import com.disasterreliefnet.inventory.dto.UpdateWarehouseRequest;
import com.disasterreliefnet.inventory.dto.WarehouseResponse;
import com.disasterreliefnet.inventory.mapper.WarehouseMapper;
import com.disasterreliefnet.inventory.repository.WarehouseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WarehouseServiceTest {

    @Mock
    private WarehouseRepository warehouseRepository;

    private final WarehouseMapper warehouseMapper = Mappers.getMapper(WarehouseMapper.class);
    private WarehouseService warehouseService;

    private AddressDto sampleAddress;

    @BeforeEach
    void setUp() {
        warehouseService = new WarehouseService(warehouseRepository, warehouseMapper);
        sampleAddress = new AddressDto("100 Relief Way", "Miami", "FL", "US", "33101");
    }

    @Test
    void shouldCreateWarehouseSuccessfully() {
        CreateWarehouseRequest request = new CreateWarehouseRequest(
            "Central Depot",
            sampleAddress,
            new BigDecimal("5000.00")
        );

        when(warehouseRepository.existsByNameIgnoreCaseAndDeletedAtIsNull("Central Depot")).thenReturn(false);
        when(warehouseRepository.save(any(Warehouse.class))).thenAnswer(invocation -> {
            Warehouse w = invocation.getArgument(0);
            w.setCreatedAt(Instant.now());
            w.setUpdatedAt(Instant.now());
            return w;
        });

        WarehouseResponse response = warehouseService.createWarehouse(request);

        assertNotNull(response);
        assertEquals("Central Depot", response.name());
        assertEquals("Miami", response.address().city());
        assertEquals(new BigDecimal("5000.00"), response.capacity());
        assertTrue(response.active());
        verify(warehouseRepository).save(any(Warehouse.class));
    }

    @Test
    void shouldRejectWarehouseWithDuplicateName() {
        CreateWarehouseRequest request = new CreateWarehouseRequest(
            "Central Depot",
            sampleAddress,
            new BigDecimal("5000.00")
        );

        when(warehouseRepository.existsByNameIgnoreCaseAndDeletedAtIsNull("Central Depot")).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> warehouseService.createWarehouse(request));
        verify(warehouseRepository, never()).save(any());
    }

    @Test
    void shouldGetWarehouseById() {
        UUID warehouseId = UUID.randomUUID();
        Warehouse warehouse = Warehouse.builder()
            .warehouseId(warehouseId)
            .name("East Hub")
            .street("200 Main St")
            .city("Atlanta")
            .state("GA")
            .country("US")
            .zip("30301")
            .capacityCbm(new BigDecimal("3000.00"))
            .active(true)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        when(warehouseRepository.findByWarehouseIdAndDeletedAtIsNull(warehouseId)).thenReturn(Optional.of(warehouse));

        WarehouseResponse response = warehouseService.getWarehouse(warehouseId);

        assertNotNull(response);
        assertEquals("East Hub", response.name());
        assertEquals("Atlanta", response.address().city());
    }

    @Test
    void shouldThrowWhenWarehouseNotFound() {
        UUID warehouseId = UUID.randomUUID();
        when(warehouseRepository.findByWarehouseIdAndDeletedAtIsNull(warehouseId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> warehouseService.getWarehouse(warehouseId));
    }

    @Test
    void shouldListWarehousesWithPagination() {
        Pageable pageable = PageRequest.of(0, 10);
        Warehouse warehouse = Warehouse.builder()
            .warehouseId(UUID.randomUUID())
            .name("North Hub")
            .active(true)
            .build();

        when(warehouseRepository.findByDeletedAtIsNull(pageable))
            .thenReturn(new PageImpl<>(List.of(warehouse), pageable, 1));

        Page<WarehouseResponse> result = warehouseService.getWarehouses(pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("North Hub", result.getContent().getFirst().name());
    }

    @Test
    void shouldUpdateWarehouse() {
        UUID warehouseId = UUID.randomUUID();
        Warehouse warehouse = Warehouse.builder()
            .warehouseId(warehouseId)
            .name("Old Name")
            .capacityCbm(new BigDecimal("1000"))
            .active(true)
            .build();

        when(warehouseRepository.findByWarehouseIdAndDeletedAtIsNull(warehouseId)).thenReturn(Optional.of(warehouse));
        when(warehouseRepository.existsByNameIgnoreCaseAndDeletedAtIsNull("New Name")).thenReturn(false);
        when(warehouseRepository.save(any(Warehouse.class))).thenAnswer(i -> i.getArgument(0));

        UpdateWarehouseRequest updateRequest = new UpdateWarehouseRequest(
            "New Name",
            sampleAddress,
            new BigDecimal("2500")
        );

        WarehouseResponse response = warehouseService.updateWarehouse(warehouseId, updateRequest);

        assertEquals("New Name", response.name());
        assertEquals(new BigDecimal("2500"), response.capacity());
    }

    @Test
    void shouldDeactivateWarehouse() {
        UUID warehouseId = UUID.randomUUID();
        Warehouse warehouse = Warehouse.builder()
            .warehouseId(warehouseId)
            .name("Depot")
            .active(true)
            .build();

        when(warehouseRepository.findByWarehouseIdAndDeletedAtIsNull(warehouseId)).thenReturn(Optional.of(warehouse));

        warehouseService.deactivateWarehouse(warehouseId);

        assertFalse(warehouse.isActive());
        assertNotNull(warehouse.getDeletedAt());
        verify(warehouseRepository).save(warehouse);
    }
}
