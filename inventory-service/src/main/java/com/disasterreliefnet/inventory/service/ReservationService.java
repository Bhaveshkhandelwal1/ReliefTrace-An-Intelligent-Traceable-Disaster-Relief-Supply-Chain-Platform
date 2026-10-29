package com.disasterreliefnet.inventory.service;

import com.disasterreliefnet.common.dto.DeliveryAddressDto;
import com.disasterreliefnet.common.enums.FailureReason;
import com.disasterreliefnet.common.enums.ReservationStatus;
import com.disasterreliefnet.common.event.KafkaTopics;
import com.disasterreliefnet.common.event.payload.AllocationApprovedPayload;
import com.disasterreliefnet.common.event.payload.StockReservationFailedPayload;
import com.disasterreliefnet.common.event.payload.StockReservedPayload;
import com.disasterreliefnet.common.exception.BusinessRuleException;
import com.disasterreliefnet.common.exception.ResourceNotFoundException;
import com.disasterreliefnet.common.outbox.OutboxService;
import com.disasterreliefnet.inventory.domain.Item;
import com.disasterreliefnet.inventory.domain.Reservation;
import com.disasterreliefnet.inventory.domain.Warehouse;
import com.disasterreliefnet.inventory.dto.ReservationResponse;
import com.disasterreliefnet.inventory.mapper.ReservationMapper;
import com.disasterreliefnet.inventory.repository.ItemRepository;
import com.disasterreliefnet.inventory.repository.ReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service orchestrating stock reservations, releases, commits, and auto-release timeouts.
 */
@Service
public class ReservationService {

    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);

    private final ReservationRepository reservationRepository;
    private final ItemRepository itemRepository;
    private final WarehouseService warehouseService;
    private final ItemService itemService;
    private final OutboxService outboxService;
    private final ReservationMapper reservationMapper;
    private final long reservationTimeoutHours;

    public ReservationService(
        ReservationRepository reservationRepository,
        ItemRepository itemRepository,
        WarehouseService warehouseService,
        ItemService itemService,
        OutboxService outboxService,
        ReservationMapper reservationMapper,
        @Value("${inventory.reservation-timeout-hours:24}") long reservationTimeoutHours
    ) {
        this.reservationRepository = reservationRepository;
        this.itemRepository = itemRepository;
        this.warehouseService = warehouseService;
        this.itemService = itemService;
        this.outboxService = outboxService;
        this.reservationMapper = reservationMapper;
        this.reservationTimeoutHours = reservationTimeoutHours;
    }

    /**
     * Attempts stock reservation for an approved allocation.
     * Produces StockReserved event on success, or StockReservationFailed event on rejection.
     */
    @Transactional
    public Reservation reserveStock(AllocationApprovedPayload payload, String correlationId) {
        log.info("Processing stock reservation for allocationId={}, itemId={}, quantity={}",
            payload.allocationId(), payload.itemId(), payload.quantity());

        Optional<Item> itemOpt = itemRepository.findByItemIdAndDeletedAtIsNull(payload.itemId());
        if (itemOpt.isEmpty()) {
            log.warn("Item not found for reservation: itemId={}", payload.itemId());
            publishReservationFailed(payload, BigDecimal.ZERO, FailureReason.ITEM_NOT_FOUND, correlationId);
            return null;
        }

        Item item = itemOpt.get();

        if (item.isExpired()) {
            log.warn("Item is expired for reservation: itemId={}, expiresAt={}", item.getItemId(), item.getExpiresAt());
            publishReservationFailed(payload, item.getQuantityAvailable(), FailureReason.ITEM_EXPIRED, correlationId);
            return null;
        }

        BigDecimal available = item.getQuantityAvailable();
        if (available.compareTo(payload.quantity()) < 0) {
            log.warn("Insufficient stock for reservation: itemId={}, requested={}, available={}",
                item.getItemId(), payload.quantity(), available);
            publishReservationFailed(payload, available, FailureReason.INSUFFICIENT_STOCK, correlationId);
            return null;
        }

        // Check if allocation was already reserved
        Optional<Reservation> existingOpt = reservationRepository.findByAllocationIdAndDeletedAtIsNull(payload.allocationId());
        if (existingOpt.isPresent()) {
            log.info("Reservation already exists for allocationId={}", payload.allocationId());
            return existingOpt.get();
        }

        // Update item with optimistic locking
        item.setQuantityReserved(item.getQuantityReserved().add(payload.quantity()));
        itemRepository.save(item);

        UUID reservationId = UUID.randomUUID();
        Reservation reservation = Reservation.builder()
            .reservationId(reservationId)
            .itemId(item.getItemId())
            .allocationId(payload.allocationId())
            .quantity(payload.quantity())
            .status(ReservationStatus.CONFIRMED)
            .build();
        Reservation savedReservation = reservationRepository.save(reservation);

        // Resolve pickup address from warehouse
        Warehouse warehouse = warehouseService.findWarehouseOrThrow(item.getWarehouseId());
        DeliveryAddressDto pickupAddress = new DeliveryAddressDto(
            warehouse.getStreet() != null ? warehouse.getStreet() : "Main Logistics Warehouse",
            warehouse.getCity() != null ? warehouse.getCity() : "Relief Hub",
            warehouse.getState() != null ? warehouse.getState() : "Relief State",
            warehouse.getCountry() != null ? warehouse.getCountry() : "US",
            warehouse.getZip() != null ? warehouse.getZip() : "00000"
        );

        StockReservedPayload reservedPayload = new StockReservedPayload(
            reservationId,
            payload.allocationId(),
            payload.requestId(),
            payload.receiverId(),
            item.getItemId(),
            warehouse.getWarehouseId(),
            payload.quantity(),
            payload.unit(),
            pickupAddress,
            payload.deliveryAddress()
        );

        outboxService.enqueue(
            KafkaTopics.STOCK_RESERVED,
            "RESERVATION",
            reservationId,
            "StockReserved",
            reservedPayload,
            correlationId
        );

        log.info("Stock reserved successfully: reservationId={}, allocationId={}", reservationId, payload.allocationId());
        itemService.checkAndPublishLowStock(item);
        return savedReservation;
    }

    private void publishReservationFailed(
        AllocationApprovedPayload payload,
        BigDecimal availableQty,
        FailureReason reason,
        String correlationId
    ) {
        StockReservationFailedPayload failedPayload = new StockReservationFailedPayload(
            payload.allocationId(),
            payload.requestId(),
            payload.itemId(),
            payload.quantity(),
            availableQty,
            reason
        );
        outboxService.enqueue(
            KafkaTopics.STOCK_RESERVATION_FAILED,
            "ALLOCATION",
            payload.allocationId(),
            "StockReservationFailed",
            failedPayload,
            correlationId
        );
    }

    /**
     * Releases an active stock reservation (e.g., cancelled shipment or manual release).
     */
    @Transactional
    public void releaseStock(UUID reservationId, String reason) {
        Reservation reservation = reservationRepository.findByReservationIdAndDeletedAtIsNull(reservationId)
            .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + reservationId));

        if (reservation.getStatus() == ReservationStatus.CONFIRMED || reservation.getStatus() == ReservationStatus.PENDING) {
            Item item = itemRepository.findByItemIdAndDeletedAtIsNull(reservation.getItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + reservation.getItemId()));

            BigDecimal currentReserved = item.getQuantityReserved();
            BigDecimal newReserved = currentReserved.subtract(reservation.getQuantity());
            item.setQuantityReserved(newReserved.compareTo(BigDecimal.ZERO) > 0 ? newReserved : BigDecimal.ZERO);
            itemRepository.save(item);

            reservation.setStatus(ReservationStatus.RELEASED);
            reservationRepository.save(reservation);

            log.info("Stock reservation {} released. Reason: {}", reservationId, reason);
        } else {
            log.info("Reservation {} is in status {}, release not required.", reservationId, reservation.getStatus());
        }
    }

    /**
     * Releases stock reservation by allocation ID (invoked on ShipmentCancelled).
     */
    @Transactional
    public void releaseStockByAllocationId(UUID allocationId, String reason) {
        reservationRepository.findByAllocationIdAndDeletedAtIsNull(allocationId)
            .ifPresent(res -> releaseStock(res.getReservationId(), reason));
    }

    /**
     * Commits reserved stock when delivery is confirmed (invoked on ShipmentDelivered).
     */
    @Transactional
    public void commitStock(UUID reservationId, UUID allocationId) {
        Optional<Reservation> resOpt = reservationId != null
            ? reservationRepository.findByReservationIdAndDeletedAtIsNull(reservationId)
            : reservationRepository.findByAllocationIdAndDeletedAtIsNull(allocationId);

        if (resOpt.isEmpty()) {
            log.warn("No active reservation found to commit for reservationId={}, allocationId={}", reservationId, allocationId);
            return;
        }

        Reservation reservation = resOpt.get();
        if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
            Item item = itemRepository.findByItemIdAndDeletedAtIsNull(reservation.getItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + reservation.getItemId()));

            BigDecimal qty = reservation.getQuantity();
            BigDecimal newOnHand = item.getQuantityOnHand().subtract(qty);
            BigDecimal newReserved = item.getQuantityReserved().subtract(qty);

            item.setQuantityOnHand(newOnHand.compareTo(BigDecimal.ZERO) > 0 ? newOnHand : BigDecimal.ZERO);
            item.setQuantityReserved(newReserved.compareTo(BigDecimal.ZERO) > 0 ? newReserved : BigDecimal.ZERO);
            itemRepository.save(item);

            reservation.setStatus(ReservationStatus.CONSUMED);
            reservationRepository.save(reservation);

            log.info("Committed stock for reservation {}: item {} consumed quantity {}",
                reservation.getReservationId(), item.getItemId(), qty);
        }
    }

    /**
     * Scheduled cleanup that automatically releases unfulfilled reservations exceeding the timeout duration.
     */
    @Scheduled(fixedDelayString = "${inventory.auto-release-delay-ms:60000}")
    @Transactional
    public void autoReleaseExpiredReservations() {
        Instant cutoff = Instant.now().minus(Duration.ofHours(reservationTimeoutHours));
        List<Reservation> expiredReservations = reservationRepository.findByStatusInAndCreatedAtBeforeAndDeletedAtIsNull(
            List.of(ReservationStatus.CONFIRMED, ReservationStatus.PENDING),
            cutoff
        );

        if (!expiredReservations.isEmpty()) {
            log.info("Found {} expired reservations exceeding timeout of {} hours. Auto-releasing...",
                expiredReservations.size(), reservationTimeoutHours);

            for (Reservation reservation : expiredReservations) {
                try {
                    releaseStock(reservation.getReservationId(), "Reservation auto-released due to timeout (" + reservationTimeoutHours + "h)");
                } catch (Exception e) {
                    log.error("Failed to auto-release reservation {}: {}", reservation.getReservationId(), e.getMessage());
                }
            }
        }
    }

    @Transactional(readOnly = true)
    public Page<ReservationResponse> getReservations(UUID allocationId, Pageable pageable) {
        Page<Reservation> reservations;
        if (allocationId != null) {
            reservations = reservationRepository.findByAllocationIdAndDeletedAtIsNull(allocationId, pageable);
        } else {
            reservations = reservationRepository.findByDeletedAtIsNull(pageable);
        }
        return reservations.map(reservationMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ReservationResponse getReservation(UUID reservationId) {
        Reservation reservation = reservationRepository.findByReservationIdAndDeletedAtIsNull(reservationId)
            .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + reservationId));
        return reservationMapper.toResponse(reservation);
    }
}
