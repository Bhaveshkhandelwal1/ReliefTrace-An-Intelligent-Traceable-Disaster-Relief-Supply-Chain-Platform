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
import com.disasterreliefnet.inventory.dto.UpdateItemRequest;
import com.disasterreliefnet.inventory.mapper.ItemMapper;
import com.disasterreliefnet.inventory.repository.ItemRepository;
import com.disasterreliefnet.inventory.repository.StockAdjustmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Service managing inventory stock, catalog items, and stock adjustments.
 */
@Service
public class ItemService {

    private static final Logger log = LoggerFactory.getLogger(ItemService.class);

    private final ItemRepository itemRepository;
    private final StockAdjustmentRepository stockAdjustmentRepository;
    private final WarehouseService warehouseService;
    private final ItemMapper itemMapper;
    private final OutboxService outboxService;
    private final BigDecimal lowStockThreshold;

    public ItemService(
        ItemRepository itemRepository,
        StockAdjustmentRepository stockAdjustmentRepository,
        WarehouseService warehouseService,
        ItemMapper itemMapper,
        OutboxService outboxService,
        @Value("${inventory.low-stock-threshold:10.0}") BigDecimal lowStockThreshold
    ) {
        this.itemRepository = itemRepository;
        this.stockAdjustmentRepository = stockAdjustmentRepository;
        this.warehouseService = warehouseService;
        this.itemMapper = itemMapper;
        this.outboxService = outboxService;
        this.lowStockThreshold = lowStockThreshold;
    }

    @Transactional
    public ItemResponse createItem(CreateItemRequest request) {
        Warehouse warehouse = warehouseService.findWarehouseOrThrow(request.warehouseId());
        if (!warehouse.isActive()) {
            throw new BusinessRuleException("Cannot add items to inactive warehouse: " + request.warehouseId());
        }

        if (request.expiresAt() != null && request.expiresAt().isBefore(Instant.now())) {
            throw new BusinessRuleException("Cannot register item with an expiry date in the past");
        }

        if (itemRepository.existsByWarehouseIdAndNameIgnoreCaseAndDeletedAtIsNull(request.warehouseId(), request.name())) {
            throw new BusinessRuleException("Item with name '" + request.name() + "' already exists in warehouse " + request.warehouseId());
        }

        Item item = itemMapper.toEntity(request);
        Item saved = itemRepository.save(item);
        log.info("Created inventory item: id={}, name={}, warehouseId={}, onHand={}",
            saved.getItemId(), saved.getName(), saved.getWarehouseId(), saved.getQuantityOnHand());

        checkAndPublishLowStock(saved);
        return itemMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ItemResponse getItem(UUID itemId) {
        Item item = findItemOrThrow(itemId);
        return itemMapper.toResponse(item);
    }

    @Transactional(readOnly = true)
    public Page<ItemResponse> getItems(AidCategory category, UUID warehouseId, Pageable pageable) {
        Page<Item> items;
        if (category != null && warehouseId != null) {
            items = itemRepository.findByCategoryAndWarehouseIdAndDeletedAtIsNull(category, warehouseId, pageable);
        } else if (category != null) {
            items = itemRepository.findByCategoryAndDeletedAtIsNull(category, pageable);
        } else if (warehouseId != null) {
            items = itemRepository.findByWarehouseIdAndDeletedAtIsNull(warehouseId, pageable);
        } else {
            items = itemRepository.findByDeletedAtIsNull(pageable);
        }
        return items.map(itemMapper::toResponse);
    }

    @Transactional
    public ItemResponse updateItem(UUID itemId, UpdateItemRequest request) {
        Item item = findItemOrThrow(itemId);

        if (request.expiresAt() != null && request.expiresAt().isBefore(Instant.now())) {
            throw new BusinessRuleException("Cannot set expiry date to the past");
        }

        itemMapper.updateEntityFromDto(request, item);
        Item updated = itemRepository.save(item);
        log.info("Updated item: id={}, name={}, expiresAt={}", updated.getItemId(), updated.getName(), updated.getExpiresAt());
        return itemMapper.toResponse(updated);
    }

    @Transactional
    public ItemResponse adjustStock(UUID itemId, AdjustStockRequest request, String adjustedBy) {
        Item item = findItemOrThrow(itemId);

        if (item.isExpired()) {
            throw new BusinessRuleException("Cannot adjust stock for expired item: " + itemId);
        }

        BigDecimal currentOnHand = item.getQuantityOnHand();
        BigDecimal newOnHand = currentOnHand.add(request.adjustment());

        if (newOnHand.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("Insufficient stock on hand: current=" + currentOnHand + ", requested adjustment=" + request.adjustment());
        }

        if (newOnHand.compareTo(item.getQuantityReserved()) < 0) {
            throw new BusinessRuleException("Cannot reduce stock below reserved quantity: reserved=" + item.getQuantityReserved() + ", would-be on-hand=" + newOnHand);
        }

        item.setQuantityOnHand(newOnHand);
        Item saved = itemRepository.save(item);

        StockAdjustment adjustment = StockAdjustment.builder()
            .adjustmentId(UUID.randomUUID())
            .itemId(itemId)
            .adjustment(request.adjustment())
            .reason(request.reason())
            .adjustedBy(adjustedBy != null ? adjustedBy : "SYSTEM")
            .build();
        stockAdjustmentRepository.save(adjustment);

        log.info("Stock adjusted for item {}: oldOnHand={}, newOnHand={}, adjustment={}, by={}",
            itemId, currentOnHand, newOnHand, request.adjustment(), adjustedBy);

        checkAndPublishLowStock(saved);
        return itemMapper.toResponse(saved);
    }

    public void checkAndPublishLowStock(Item item) {
        BigDecimal available = item.getQuantityAvailable();
        if (available.compareTo(lowStockThreshold) <= 0) {
            log.warn("Item stock level low! itemId={}, name={}, available={}, threshold={}",
                item.getItemId(), item.getName(), available, lowStockThreshold);
            StockLowPayload payload = new StockLowPayload(
                item.getItemId(),
                item.getWarehouseId(),
                item.getName(),
                item.getCategory().name(),
                available,
                lowStockThreshold,
                item.getUnit()
            );
            outboxService.enqueue(
                KafkaTopics.STOCK_LOW,
                "ITEM",
                item.getItemId(),
                "StockLow",
                payload,
                UUID.randomUUID().toString()
            );
        }
    }

    public Item findItemOrThrow(UUID itemId) {
        return itemRepository.findByItemIdAndDeletedAtIsNull(itemId)
            .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + itemId));
    }
}
