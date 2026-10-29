package com.disasterreliefnet.inventory.event;

import com.disasterreliefnet.common.dto.EventEnvelope;
import com.disasterreliefnet.common.enums.AidCategory;
import com.disasterreliefnet.common.enums.DonationType;
import com.disasterreliefnet.common.event.KafkaTopics;
import com.disasterreliefnet.common.event.payload.AllocationApprovedPayload;
import com.disasterreliefnet.common.event.payload.DonationRegisteredPayload;
import com.disasterreliefnet.common.event.payload.ShipmentCancelledPayload;
import com.disasterreliefnet.common.event.payload.ShipmentDeliveredPayload;
import com.disasterreliefnet.common.idempotency.IdempotentEventConsumer;
import com.disasterreliefnet.inventory.domain.Warehouse;
import com.disasterreliefnet.inventory.dto.CreateItemRequest;
import com.disasterreliefnet.inventory.repository.WarehouseRepository;
import com.disasterreliefnet.inventory.service.ItemService;
import com.disasterreliefnet.inventory.service.ReservationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Idempotent Kafka consumer listening for allocation approvals, shipment confirmations,
 * cancellations, and in-kind donation registrations.
 */
@Component
public class InventoryEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(InventoryEventConsumer.class);

    private final ReservationService reservationService;
    private final ItemService itemService;
    private final WarehouseRepository warehouseRepository;
    private final IdempotentEventConsumer idempotentEventConsumer;
    private final ObjectMapper objectMapper;

    public InventoryEventConsumer(
        ReservationService reservationService,
        ItemService itemService,
        WarehouseRepository warehouseRepository,
        IdempotentEventConsumer idempotentEventConsumer,
        ObjectMapper objectMapper
    ) {
        this.reservationService = reservationService;
        this.itemService = itemService;
        this.warehouseRepository = warehouseRepository;
        this.idempotentEventConsumer = idempotentEventConsumer;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = KafkaTopics.ALLOCATION_APPROVED, groupId = "${spring.kafka.consumer.group-id:inventory-service-group}")
    public void handleAllocationApproved(String message) {
        try {
            EventEnvelope<AllocationApprovedPayload> envelope = objectMapper.readValue(
                message,
                new TypeReference<>() {}
            );

            idempotentEventConsumer.execute(envelope.eventId(), envelope.eventType(), () -> {
                log.info("Handling AllocationApproved event {} for allocation {}", envelope.eventId(), envelope.payload().allocationId());
                reservationService.reserveStock(envelope.payload(), envelope.correlationId());
            });
        } catch (Exception ex) {
            log.error("Failed to process AllocationApproved event: {}", ex.getMessage(), ex);
            throw new IllegalArgumentException("Invalid AllocationApproved event payload", ex);
        }
    }

    @KafkaListener(topics = KafkaTopics.SHIPMENT_DELIVERED, groupId = "${spring.kafka.consumer.group-id:inventory-service-group}")
    public void handleShipmentDelivered(String message) {
        try {
            EventEnvelope<ShipmentDeliveredPayload> envelope = objectMapper.readValue(
                message,
                new TypeReference<>() {}
            );

            idempotentEventConsumer.execute(envelope.eventId(), envelope.eventType(), () -> {
                log.info("Handling ShipmentDelivered event {} for allocation {}", envelope.eventId(), envelope.payload().allocationId());
                reservationService.commitStock(envelope.payload().reservationId(), envelope.payload().allocationId());
            });
        } catch (Exception ex) {
            log.error("Failed to process ShipmentDelivered event: {}", ex.getMessage(), ex);
            throw new IllegalArgumentException("Invalid ShipmentDelivered event payload", ex);
        }
    }

    @KafkaListener(topics = KafkaTopics.SHIPMENT_CANCELLED, groupId = "${spring.kafka.consumer.group-id:inventory-service-group}")
    public void handleShipmentCancelled(String message) {
        try {
            EventEnvelope<ShipmentCancelledPayload> envelope = objectMapper.readValue(
                message,
                new TypeReference<>() {}
            );

            idempotentEventConsumer.execute(envelope.eventId(), envelope.eventType(), () -> {
                log.info("Handling ShipmentCancelled event {} for allocation {}", envelope.eventId(), envelope.payload().allocationId());
                reservationService.releaseStockByAllocationId(envelope.payload().allocationId(), envelope.payload().reason());
            });
        } catch (Exception ex) {
            log.error("Failed to process ShipmentCancelled event: {}", ex.getMessage(), ex);
            throw new IllegalArgumentException("Invalid ShipmentCancelled event payload", ex);
        }
    }

    @KafkaListener(topics = KafkaTopics.DONATION_REGISTERED, groupId = "${spring.kafka.consumer.group-id:inventory-service-group}")
    public void handleDonationRegistered(String message) {
        try {
            EventEnvelope<DonationRegisteredPayload> envelope = objectMapper.readValue(
                message,
                new TypeReference<>() {}
            );

            idempotentEventConsumer.execute(envelope.eventId(), envelope.eventType(), () -> {
                DonationRegisteredPayload payload = envelope.payload();
                if (payload.donationType() == DonationType.IN_KIND) {
                    log.info("Processing IN_KIND donation {} into inventory: item='{}', qty={}",
                        payload.donationId(), payload.itemDescription(), payload.quantityKg());

                    List<Warehouse> warehouses = warehouseRepository.findAll();
                    if (!warehouses.isEmpty()) {
                        Warehouse primaryWarehouse = warehouses.getFirst();
                        BigDecimal qty = payload.quantityKg() != null ? payload.quantityKg() : BigDecimal.ONE;
                        String itemName = payload.itemDescription() != null ? payload.itemDescription() : "In-Kind Relief Supplies";

                        try {
                            CreateItemRequest itemRequest = new CreateItemRequest(
                                primaryWarehouse.getWarehouseId(),
                                itemName + " (Donation " + payload.donationId().toString().substring(0, 8) + ")",
                                AidCategory.OTHER,
                                "kg",
                                qty,
                                null
                            );
                            itemService.createItem(itemRequest);
                        } catch (Exception ex) {
                            log.warn("Could not register auto-created in-kind item: {}", ex.getMessage());
                        }
                    }
                }
            });
        } catch (Exception ex) {
            log.error("Failed to process DonationRegistered event: {}", ex.getMessage(), ex);
            throw new IllegalArgumentException("Invalid DonationRegistered event payload", ex);
        }
    }
}
