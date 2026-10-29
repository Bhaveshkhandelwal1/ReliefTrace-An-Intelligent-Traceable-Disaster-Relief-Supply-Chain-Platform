package com.disasterreliefnet.inventory.service;

import com.disasterreliefnet.common.dto.DeliveryAddressDto;
import com.disasterreliefnet.common.enums.AidCategory;
import com.disasterreliefnet.common.enums.FailureReason;
import com.disasterreliefnet.common.enums.ReservationStatus;
import com.disasterreliefnet.common.event.KafkaTopics;
import com.disasterreliefnet.common.event.payload.AllocationApprovedPayload;
import com.disasterreliefnet.common.event.payload.StockReservationFailedPayload;
import com.disasterreliefnet.common.event.payload.StockReservedPayload;
import com.disasterreliefnet.common.outbox.OutboxService;
import com.disasterreliefnet.inventory.domain.Item;
import com.disasterreliefnet.inventory.domain.Reservation;
import com.disasterreliefnet.inventory.domain.Warehouse;
import com.disasterreliefnet.inventory.mapper.ReservationMapper;
import com.disasterreliefnet.inventory.repository.ItemRepository;
import com.disasterreliefnet.inventory.repository.ReservationRepository;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private WarehouseService warehouseService;

    @Mock
    private ItemService itemService;

    @Mock
    private OutboxService outboxService;

    private final ReservationMapper reservationMapper = Mappers.getMapper(ReservationMapper.class);
    private ReservationService reservationService;

    private UUID itemId;
    private UUID warehouseId;
    private UUID allocationId;
    private AllocationApprovedPayload approvedPayload;

    @BeforeEach
    void setUp() {
        reservationService = new ReservationService(
            reservationRepository,
            itemRepository,
            warehouseService,
            itemService,
            outboxService,
            reservationMapper,
            24L
        );

        itemId = UUID.randomUUID();
        warehouseId = UUID.randomUUID();
        allocationId = UUID.randomUUID();

        DeliveryAddressDto deliveryAddress = new DeliveryAddressDto("123 Victim Road", "Orlando", "FL", "US", "32801");
        approvedPayload = new AllocationApprovedPayload(
            allocationId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            itemId,
            warehouseId,
            new BigDecimal("20.0"),
            "kg",
            "coordinator_alice",
            deliveryAddress
        );
    }

    @Test
    void shouldReserveStockSuccessfullyWhenAvailable() {
        Item item = Item.builder()
            .itemId(itemId)
            .warehouseId(warehouseId)
            .name("Rice 5kg")
            .category(AidCategory.FOOD)
            .unit("kg")
            .quantityOnHand(new BigDecimal("100.0"))
            .quantityReserved(new BigDecimal("10.0"))
            .expiresAt(Instant.now().plus(60, ChronoUnit.DAYS))
            .build();

        Warehouse warehouse = Warehouse.builder()
            .warehouseId(warehouseId)
            .name("Central Storage")
            .street("500 Logistics Blvd")
            .city("Tampa")
            .state("FL")
            .country("US")
            .zip("33601")
            .active(true)
            .build();

        when(itemRepository.findByItemIdAndDeletedAtIsNull(itemId)).thenReturn(Optional.of(item));
        when(reservationRepository.findByAllocationIdAndDeletedAtIsNull(allocationId)).thenReturn(Optional.empty());
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(i -> i.getArgument(0));
        when(warehouseService.findWarehouseOrThrow(warehouseId)).thenReturn(warehouse);

        Reservation reservation = reservationService.reserveStock(approvedPayload, "corr-123");

        assertNotNull(reservation);
        assertEquals(ReservationStatus.CONFIRMED, reservation.getStatus());
        assertEquals(new BigDecimal("20.0"), reservation.getQuantity());

        // Verify item quantityReserved was incremented from 10 to 30
        assertEquals(new BigDecimal("30.0"), item.getQuantityReserved());
        verify(itemRepository).save(item);

        // Verify StockReserved event was enqueued
        ArgumentCaptor<StockReservedPayload> eventCaptor = ArgumentCaptor.forClass(StockReservedPayload.class);
        verify(outboxService).enqueue(
            eq(KafkaTopics.STOCK_RESERVED),
            eq("RESERVATION"),
            any(UUID.class),
            eq("StockReserved"),
            eventCaptor.capture(),
            eq("corr-123")
        );

        StockReservedPayload published = eventCaptor.getValue();
        assertEquals(allocationId, published.allocationId());
        assertEquals(new BigDecimal("20.0"), published.quantity());
        assertEquals("Tampa", published.pickupAddress().city());
    }

    @Test
    void shouldFailReservationWhenItemNotFound() {
        when(itemRepository.findByItemIdAndDeletedAtIsNull(itemId)).thenReturn(Optional.empty());

        Reservation reservation = reservationService.reserveStock(approvedPayload, "corr-404");

        assertNull(reservation);
        verify(reservationRepository, never()).save(any());

        ArgumentCaptor<StockReservationFailedPayload> captor = ArgumentCaptor.forClass(StockReservationFailedPayload.class);
        verify(outboxService).enqueue(
            eq(KafkaTopics.STOCK_RESERVATION_FAILED),
            eq("ALLOCATION"),
            eq(allocationId),
            eq("StockReservationFailed"),
            captor.capture(),
            eq("corr-404")
        );

        assertEquals(FailureReason.ITEM_NOT_FOUND, captor.getValue().failureReason());
    }

    @Test
    void shouldFailReservationWhenItemIsExpired() {
        Item expiredItem = Item.builder()
            .itemId(itemId)
            .warehouseId(warehouseId)
            .quantityOnHand(new BigDecimal("100.0"))
            .quantityReserved(BigDecimal.ZERO)
            .expiresAt(Instant.now().minus(5, ChronoUnit.DAYS))
            .build();

        when(itemRepository.findByItemIdAndDeletedAtIsNull(itemId)).thenReturn(Optional.of(expiredItem));

        Reservation reservation = reservationService.reserveStock(approvedPayload, "corr-exp");

        assertNull(reservation);
        ArgumentCaptor<StockReservationFailedPayload> captor = ArgumentCaptor.forClass(StockReservationFailedPayload.class);
        verify(outboxService).enqueue(
            eq(KafkaTopics.STOCK_RESERVATION_FAILED),
            eq("ALLOCATION"),
            eq(allocationId),
            eq("StockReservationFailed"),
            captor.capture(),
            eq("corr-exp")
        );
        assertEquals(FailureReason.ITEM_EXPIRED, captor.getValue().failureReason());
    }

    @Test
    void shouldFailReservationWhenInsufficientStock() {
        Item lowStockItem = Item.builder()
            .itemId(itemId)
            .warehouseId(warehouseId)
            .quantityOnHand(new BigDecimal("15.0")) // available 10 < requested 20
            .quantityReserved(new BigDecimal("5.0"))
            .build();

        when(itemRepository.findByItemIdAndDeletedAtIsNull(itemId)).thenReturn(Optional.of(lowStockItem));

        Reservation reservation = reservationService.reserveStock(approvedPayload, "corr-ins");

        assertNull(reservation);
        ArgumentCaptor<StockReservationFailedPayload> captor = ArgumentCaptor.forClass(StockReservationFailedPayload.class);
        verify(outboxService).enqueue(
            eq(KafkaTopics.STOCK_RESERVATION_FAILED),
            eq("ALLOCATION"),
            eq(allocationId),
            eq("StockReservationFailed"),
            captor.capture(),
            eq("corr-ins")
        );
        assertEquals(FailureReason.INSUFFICIENT_STOCK, captor.getValue().failureReason());
    }

    @Test
    void shouldReleaseReservedStock() {
        UUID reservationId = UUID.randomUUID();
        Reservation reservation = Reservation.builder()
            .reservationId(reservationId)
            .itemId(itemId)
            .quantity(new BigDecimal("25.0"))
            .status(ReservationStatus.CONFIRMED)
            .build();

        Item item = Item.builder()
            .itemId(itemId)
            .quantityOnHand(new BigDecimal("100.0"))
            .quantityReserved(new BigDecimal("40.0"))
            .build();

        when(reservationRepository.findByReservationIdAndDeletedAtIsNull(reservationId)).thenReturn(Optional.of(reservation));
        when(itemRepository.findByItemIdAndDeletedAtIsNull(itemId)).thenReturn(Optional.of(item));

        reservationService.releaseStock(reservationId, "Allocation canceled");

        assertEquals(ReservationStatus.RELEASED, reservation.getStatus());
        assertEquals(new BigDecimal("15.0"), item.getQuantityReserved());
        verify(itemRepository).save(item);
        verify(reservationRepository).save(reservation);
    }

    @Test
    void shouldCommitStockWhenShipmentDelivered() {
        UUID reservationId = UUID.randomUUID();
        Reservation reservation = Reservation.builder()
            .reservationId(reservationId)
            .itemId(itemId)
            .quantity(new BigDecimal("15.0"))
            .status(ReservationStatus.CONFIRMED)
            .build();

        Item item = Item.builder()
            .itemId(itemId)
            .quantityOnHand(new BigDecimal("50.0"))
            .quantityReserved(new BigDecimal("15.0"))
            .build();

        when(reservationRepository.findByReservationIdAndDeletedAtIsNull(reservationId)).thenReturn(Optional.of(reservation));
        when(itemRepository.findByItemIdAndDeletedAtIsNull(itemId)).thenReturn(Optional.of(item));

        reservationService.commitStock(reservationId, allocationId);

        assertEquals(ReservationStatus.CONSUMED, reservation.getStatus());
        assertEquals(new BigDecimal("35.0"), item.getQuantityOnHand());
        assertEquals(BigDecimal.ZERO, item.getQuantityReserved());
        verify(itemRepository).save(item);
    }

    @Test
    void shouldAutoReleaseTimedOutReservations() {
        UUID reservationId = UUID.randomUUID();
        Reservation expiredReservation = Reservation.builder()
            .reservationId(reservationId)
            .itemId(itemId)
            .quantity(new BigDecimal("10.0"))
            .status(ReservationStatus.CONFIRMED)
            .createdAt(Instant.now().minus(30, ChronoUnit.HOURS))
            .build();

        Item item = Item.builder()
            .itemId(itemId)
            .quantityOnHand(new BigDecimal("50.0"))
            .quantityReserved(new BigDecimal("10.0"))
            .build();

        when(reservationRepository.findByStatusInAndCreatedAtBeforeAndDeletedAtIsNull(any(), any()))
            .thenReturn(List.of(expiredReservation));
        when(reservationRepository.findByReservationIdAndDeletedAtIsNull(reservationId))
            .thenReturn(Optional.of(expiredReservation));
        when(itemRepository.findByItemIdAndDeletedAtIsNull(itemId))
            .thenReturn(Optional.of(item));

        reservationService.autoReleaseExpiredReservations();

        assertEquals(ReservationStatus.RELEASED, expiredReservation.getStatus());
        assertEquals(BigDecimal.ZERO, item.getQuantityReserved());
    }
}
