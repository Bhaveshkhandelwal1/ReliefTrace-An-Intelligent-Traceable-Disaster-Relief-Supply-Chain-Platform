package com.disasterreliefnet.inventory.service;

import com.disasterreliefnet.common.enums.AidCategory;
import com.disasterreliefnet.common.event.KafkaTopics;
import com.disasterreliefnet.common.event.payload.StockLowPayload;
import com.disasterreliefnet.common.exception.BusinessRuleException;
import com.disasterreliefnet.common.exception.ResourceNotFoundException;
import com.disasterreliefnet.common.outbox.OutboxService;
import com.disasterreliefnet.inventory.domain.Item;
import com.disasterreliefnet.inventory.domain.StockAdjustment;
import com.disasterreliefnet.inventory.domain.Warehouse;
import com.disasterreliefnet.inventory.dto.AdjustStockRequest;
import com.disasterreliefnet.inventory.dto.CreateItemRequest;
import com.disasterreliefnet.inventory.dto.ItemResponse;
import com.disasterreliefnet.inventory.mapper.ItemMapper;
import com.disasterreliefnet.inventory.repository.ItemRepository;
import com.disasterreliefnet.inventory.repository.StockAdjustmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private StockAdjustmentRepository stockAdjustmentRepository;

    @Mock
    private WarehouseService warehouseService;

    @Mock
    private OutboxService outboxService;

    private final ItemMapper itemMapper = Mappers.getMapper(ItemMapper.class);
    private ItemService itemService;

    private final BigDecimal lowStockThreshold = new BigDecimal("10.0");
    private UUID warehouseId;
    private Warehouse activeWarehouse;

    @BeforeEach
    void setUp() {
        itemService = new ItemService(
            itemRepository,
            stockAdjustmentRepository,
            warehouseService,
            itemMapper,
            outboxService,
            lowStockThreshold
        );
        warehouseId = UUID.randomUUID();
        activeWarehouse = Warehouse.builder()
            .warehouseId(warehouseId)
            .name("Active Depot")
            .active(true)
            .build();
    }

    @Test
    void shouldCreateItemSuccessfully() {
        CreateItemRequest request = new CreateItemRequest(
            warehouseId,
            "Bottled Water 1L",
            AidCategory.WATER,
            "bottles",
            new BigDecimal("500.00"),
            Instant.now().plus(180, ChronoUnit.DAYS)
        );

        when(warehouseService.findWarehouseOrThrow(warehouseId)).thenReturn(activeWarehouse);
        when(itemRepository.existsByWarehouseIdAndNameIgnoreCaseAndDeletedAtIsNull(warehouseId, "Bottled Water 1L")).thenReturn(false);
        when(itemRepository.save(any(Item.class))).thenAnswer(i -> {
            Item item = i.getArgument(0);
            item.setCreatedAt(Instant.now());
            item.setUpdatedAt(Instant.now());
            return item;
        });

        ItemResponse response = itemService.createItem(request);

        assertNotNull(response);
        assertEquals("Bottled Water 1L", response.name());
        assertEquals(AidCategory.WATER, response.category());
        assertEquals(new BigDecimal("500.00"), response.quantityOnHand());
        assertEquals(new BigDecimal("500.00"), response.quantityAvailable());
        verify(itemRepository).save(any(Item.class));
    }

    @Test
    void shouldRejectCreateItemWhenWarehouseIsInactive() {
        Warehouse inactiveWarehouse = Warehouse.builder()
            .warehouseId(warehouseId)
            .name("Closed Depot")
            .active(false)
            .build();
        when(warehouseService.findWarehouseOrThrow(warehouseId)).thenReturn(inactiveWarehouse);

        CreateItemRequest request = new CreateItemRequest(
            warehouseId,
            "Canned Tuna",
            AidCategory.FOOD,
            "cans",
            new BigDecimal("100"),
            null
        );

        assertThrows(BusinessRuleException.class, () -> itemService.createItem(request));
        verify(itemRepository, never()).save(any());
    }

    @Test
    void shouldRejectCreateItemWithPastExpiryDate() {
        when(warehouseService.findWarehouseOrThrow(warehouseId)).thenReturn(activeWarehouse);

        CreateItemRequest request = new CreateItemRequest(
            warehouseId,
            "Expired Milk",
            AidCategory.FOOD,
            "cartons",
            new BigDecimal("50"),
            Instant.now().minus(2, ChronoUnit.DAYS)
        );

        assertThrows(BusinessRuleException.class, () -> itemService.createItem(request));
    }

    @Test
    void shouldAdjustStockPositivelyAndSaveAudit() {
        UUID itemId = UUID.randomUUID();
        Item item = Item.builder()
            .itemId(itemId)
            .warehouseId(warehouseId)
            .name("First Aid Kits")
            .category(AidCategory.MEDICINE)
            .unit("kits")
            .quantityOnHand(new BigDecimal("20.0"))
            .quantityReserved(new BigDecimal("5.0"))
            .build();

        when(itemRepository.findByItemIdAndDeletedAtIsNull(itemId)).thenReturn(Optional.of(item));
        when(itemRepository.save(any(Item.class))).thenAnswer(i -> i.getArgument(0));

        AdjustStockRequest adjustRequest = new AdjustStockRequest(new BigDecimal("30.0"), "Shipment received");
        ItemResponse response = itemService.adjustStock(itemId, adjustRequest, "coordinator_user");

        assertEquals(new BigDecimal("50.0"), response.quantityOnHand());
        assertEquals(new BigDecimal("45.0"), response.quantityAvailable());

        ArgumentCaptor<StockAdjustment> adjustmentCaptor = ArgumentCaptor.forClass(StockAdjustment.class);
        verify(stockAdjustmentRepository).save(adjustmentCaptor.capture());
        assertEquals(new BigDecimal("30.0"), adjustmentCaptor.getValue().getAdjustment());
        assertEquals("coordinator_user", adjustmentCaptor.getValue().getAdjustedBy());
    }

    @Test
    void shouldRejectStockAdjustmentCausingNegativeStock() {
        UUID itemId = UUID.randomUUID();
        Item item = Item.builder()
            .itemId(itemId)
            .warehouseId(warehouseId)
            .name("Blankets")
            .quantityOnHand(new BigDecimal("10.0"))
            .quantityReserved(BigDecimal.ZERO)
            .build();

        when(itemRepository.findByItemIdAndDeletedAtIsNull(itemId)).thenReturn(Optional.of(item));

        AdjustStockRequest adjustRequest = new AdjustStockRequest(new BigDecimal("-15.0"), "Defective batch removed");
        assertThrows(BusinessRuleException.class, () -> itemService.adjustStock(itemId, adjustRequest, "admin"));
        verify(itemRepository, never()).save(any());
    }

    @Test
    void shouldRejectStockAdjustmentReducingBelowReservedQuantity() {
        UUID itemId = UUID.randomUUID();
        Item item = Item.builder()
            .itemId(itemId)
            .warehouseId(warehouseId)
            .name("Tents")
            .quantityOnHand(new BigDecimal("50.0"))
            .quantityReserved(new BigDecimal("40.0"))
            .build();

        when(itemRepository.findByItemIdAndDeletedAtIsNull(itemId)).thenReturn(Optional.of(item));

        // Reducing by 20 would leave 30 on hand, which is less than 40 reserved!
        AdjustStockRequest adjustRequest = new AdjustStockRequest(new BigDecimal("-20.0"), "Damaged");
        assertThrows(BusinessRuleException.class, () -> itemService.adjustStock(itemId, adjustRequest, "admin"));
    }

    @Test
    void shouldPublishLowStockEventWhenAvailableStockFallsBelowThreshold() {
        UUID itemId = UUID.randomUUID();
        Item item = Item.builder()
            .itemId(itemId)
            .warehouseId(warehouseId)
            .name("Painkillers")
            .category(AidCategory.MEDICINE)
            .unit("boxes")
            .quantityOnHand(new BigDecimal("15.0"))
            .quantityReserved(new BigDecimal("0.0"))
            .build();

        when(itemRepository.findByItemIdAndDeletedAtIsNull(itemId)).thenReturn(Optional.of(item));
        when(itemRepository.save(any(Item.class))).thenAnswer(i -> i.getArgument(0));

        // Reduce by 10 => onHand 5 <= threshold 10
        AdjustStockRequest adjustRequest = new AdjustStockRequest(new BigDecimal("-10.0"), "Distributed locally");
        itemService.adjustStock(itemId, adjustRequest, "admin");

        verify(outboxService).enqueue(
            eq(KafkaTopics.STOCK_LOW),
            eq("ITEM"),
            eq(itemId),
            eq("StockLow"),
            any(StockLowPayload.class),
            any()
        );
    }
}
