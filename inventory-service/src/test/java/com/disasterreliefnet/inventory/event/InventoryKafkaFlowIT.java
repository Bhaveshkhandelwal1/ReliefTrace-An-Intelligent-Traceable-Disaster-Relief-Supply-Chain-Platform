package com.disasterreliefnet.inventory.event;

import com.disasterreliefnet.common.dto.DeliveryAddressDto;
import com.disasterreliefnet.common.dto.EventEnvelope;
import com.disasterreliefnet.common.enums.AidCategory;
import com.disasterreliefnet.common.enums.ReservationStatus;
import com.disasterreliefnet.common.event.KafkaTopics;
import com.disasterreliefnet.common.event.payload.AllocationApprovedPayload;
import com.disasterreliefnet.common.event.payload.StockReservationFailedPayload;
import com.disasterreliefnet.common.event.payload.StockReservedPayload;
import com.disasterreliefnet.common.idempotency.ProcessedEventRepository;
import com.disasterreliefnet.common.outbox.OutboxEventRepository;
import com.disasterreliefnet.inventory.domain.Item;
import com.disasterreliefnet.inventory.domain.Reservation;
import com.disasterreliefnet.inventory.domain.Warehouse;
import com.disasterreliefnet.inventory.repository.ItemRepository;
import com.disasterreliefnet.inventory.repository.ReservationRepository;
import com.disasterreliefnet.inventory.repository.WarehouseRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {
    KafkaTopics.ALLOCATION_APPROVED,
    KafkaTopics.STOCK_RESERVED,
    KafkaTopics.STOCK_RESERVATION_FAILED,
    KafkaTopics.STOCK_LOW,
    KafkaTopics.SHIPMENT_DELIVERED,
    KafkaTopics.SHIPMENT_CANCELLED,
    KafkaTopics.DONATION_REGISTERED
})
class InventoryKafkaFlowIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("inventory_db")
        .withUsername("postgres")
        .withPassword("postgres");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @MockBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    private Warehouse persistedWarehouse;
    private Item persistedItem;

    @BeforeEach
    void setup() {
        reservationRepository.deleteAll();
        outboxEventRepository.deleteAll();
        processedEventRepository.deleteAll();
        itemRepository.deleteAll();
        warehouseRepository.deleteAll();

        persistedWarehouse = warehouseRepository.save(
            Warehouse.builder()
                .warehouseId(UUID.randomUUID())
                .name("Test Relief Depot")
                .street("1 Test Blvd")
                .city("Miami")
                .state("FL")
                .country("US")
                .zip("33101")
                .active(true)
                .build()
        );

        persistedItem = itemRepository.save(
            Item.builder()
                .itemId(UUID.randomUUID())
                .warehouseId(persistedWarehouse.getWarehouseId())
                .name("Emergency Rice 5kg")
                .category(AidCategory.FOOD)
                .unit("kg")
                .quantityOnHand(new BigDecimal("500.0"))
                .quantityReserved(BigDecimal.ZERO)
                .expiresAt(Instant.now().plusSeconds(86400 * 30))
                .build()
        );
    }

    @Test
    void shouldProcessAllocationApprovedAndCreateReservation() throws Exception {
        UUID allocationId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        DeliveryAddressDto deliveryAddress = new DeliveryAddressDto(
            "456 Victim Lane", "Orlando", "FL", "US", "32801"
        );

        AllocationApprovedPayload payload = new AllocationApprovedPayload(
            allocationId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            persistedItem.getItemId(),
            persistedWarehouse.getWarehouseId(),
            new BigDecimal("50.0"),
            "kg",
            "coordinator_bob",
            deliveryAddress
        );

        EventEnvelope<AllocationApprovedPayload> envelope = new EventEnvelope<>(
            eventId, "AllocationApproved", "ALLOCATION",
            allocationId.toString(), Instant.now(), 1, UUID.randomUUID().toString(), payload
        );

        kafkaTemplate.send(KafkaTopics.ALLOCATION_APPROVED, allocationId.toString(),
            objectMapper.writeValueAsString(envelope));

        // Wait for consumer to process the message
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Optional<Reservation> reservation = reservationRepository.findByAllocationIdAndDeletedAtIsNull(allocationId);
            assertTrue(reservation.isPresent(), "Reservation should be created after AllocationApproved event");
            assertEquals(ReservationStatus.CONFIRMED, reservation.get().getStatus());
            assertEquals(new BigDecimal("50.0"), reservation.get().getQuantity());
        });

        // Stock reserved outbox event should be enqueued
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            long outboxCount = outboxEventRepository.findTop100ByPublishedFalseOrderByCreatedAtAsc().stream()
                .filter(e -> "StockReserved".equals(e.getEventType()))
                .count();
            assertEquals(1, outboxCount, "StockReserved outbox event should be enqueued");
        });

        // Item reserved quantity should be incremented
        Item refreshed = itemRepository.findByItemIdAndDeletedAtIsNull(persistedItem.getItemId()).orElseThrow();
        assertEquals(new BigDecimal("50.0"), refreshed.getQuantityReserved());
    }

    @Test
    void shouldEnqueueStockReservationFailedWhenInsufficientStock() throws Exception {
        UUID allocationId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        DeliveryAddressDto deliveryAddress = new DeliveryAddressDto(
            "789 Emergency Rd", "Tampa", "FL", "US", "33601"
        );

        AllocationApprovedPayload payload = new AllocationApprovedPayload(
            allocationId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            persistedItem.getItemId(),
            persistedWarehouse.getWarehouseId(),
            new BigDecimal("9999.0"), // More than available
            "kg",
            "coordinator_alice",
            deliveryAddress
        );

        EventEnvelope<AllocationApprovedPayload> envelope = new EventEnvelope<>(
            eventId, "AllocationApproved", "ALLOCATION",
            allocationId.toString(), Instant.now(), 1, UUID.randomUUID().toString(), payload
        );

        kafkaTemplate.send(KafkaTopics.ALLOCATION_APPROVED, allocationId.toString(),
            objectMapper.writeValueAsString(envelope));

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            long failedCount = outboxEventRepository.findTop100ByPublishedFalseOrderByCreatedAtAsc().stream()
                .filter(e -> "StockReservationFailed".equals(e.getEventType()))
                .count();
            assertEquals(1, failedCount, "StockReservationFailed outbox event should be enqueued");
        });

        // No reservation should have been created
        Optional<Reservation> reservation = reservationRepository.findByAllocationIdAndDeletedAtIsNull(allocationId);
        assertFalse(reservation.isPresent());
    }

    @Test
    void shouldBeIdempotentWhenSameEventIsConsumedTwice() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID allocationId = UUID.randomUUID();

        DeliveryAddressDto deliveryAddress = new DeliveryAddressDto(
            "12 Idempotent St", "Jacksonville", "FL", "US", "32099"
        );

        AllocationApprovedPayload payload = new AllocationApprovedPayload(
            allocationId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            persistedItem.getItemId(),
            persistedWarehouse.getWarehouseId(),
            new BigDecimal("25.0"),
            "kg",
            "coordinator_carol",
            deliveryAddress
        );

        EventEnvelope<AllocationApprovedPayload> envelope = new EventEnvelope<>(
            eventId, "AllocationApproved", "ALLOCATION",
            allocationId.toString(), Instant.now(), 1, UUID.randomUUID().toString(), payload
        );

        String serialized = objectMapper.writeValueAsString(envelope);

        // Send same event twice
        kafkaTemplate.send(KafkaTopics.ALLOCATION_APPROVED, allocationId.toString(), serialized);
        kafkaTemplate.send(KafkaTopics.ALLOCATION_APPROVED, allocationId.toString(), serialized);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            long reservationCount = reservationRepository.findAll().stream()
                .filter(r -> allocationId.equals(r.getAllocationId()))
                .count();
            assertEquals(1, reservationCount, "Exactly one reservation should exist despite duplicate event");
        });

        Item refreshed = itemRepository.findByItemIdAndDeletedAtIsNull(persistedItem.getItemId()).orElseThrow();
        assertEquals(new BigDecimal("25.0"), refreshed.getQuantityReserved());
    }
}
