package com.disasterreliefnet.matching.service;

import com.disasterreliefnet.common.dto.DeliveryAddressDto;
import com.disasterreliefnet.common.enums.AidCategory;
import com.disasterreliefnet.common.enums.AllocationStatus;
import com.disasterreliefnet.common.enums.UrgencyLevel;
import com.disasterreliefnet.common.event.KafkaTopics;
import com.disasterreliefnet.common.event.payload.AllocationApprovedPayload;
import com.disasterreliefnet.common.event.payload.AllocationRejectedPayload;
import com.disasterreliefnet.common.outbox.OutboxService;
import com.disasterreliefnet.matching.client.InventoryClient;
import com.disasterreliefnet.matching.client.ReceiverClient;
import com.disasterreliefnet.matching.client.dto.AidRequestClientDto;
import com.disasterreliefnet.matching.client.dto.ItemClientDto;
import com.disasterreliefnet.matching.domain.Allocation;
import com.disasterreliefnet.matching.domain.MatchedRequest;
import com.disasterreliefnet.matching.dto.*;
import com.disasterreliefnet.matching.exception.AllocationNotFoundException;
import com.disasterreliefnet.matching.exception.InvalidAllocationStateException;
import com.disasterreliefnet.matching.exception.InventoryUnavailableException;
import com.disasterreliefnet.matching.mapper.MatchingMapper;
import com.disasterreliefnet.matching.repository.AllocationRepository;
import com.disasterreliefnet.matching.repository.MatchedRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Core matching service managing prioritization, proportional allocation, coordinator approval,
 * and saga orchestration with compensation for relief supplies.
 */
@Service
public class MatchingService {

    private static final Logger log = LoggerFactory.getLogger(MatchingService.class);

    private final AllocationRepository allocationRepository;
    private final MatchedRequestRepository matchedRequestRepository;
    private final InventoryClient inventoryClient;
    private final ReceiverClient receiverClient;
    private final MatchingMapper matchingMapper;
    private final OutboxService outboxService;

    @Value("${matching.saga-timeout-hours:12}")
    private int sagaTimeoutHours;

    @Value("${matching.proportional-threshold:0.8}")
    private double proportionalThreshold;

    public MatchingService(
        AllocationRepository allocationRepository,
        MatchedRequestRepository matchedRequestRepository,
        InventoryClient inventoryClient,
        ReceiverClient receiverClient,
        MatchingMapper matchingMapper,
        OutboxService outboxService
    ) {
        this.allocationRepository = allocationRepository;
        this.matchedRequestRepository = matchedRequestRepository;
        this.inventoryClient = inventoryClient;
        this.receiverClient = receiverClient;
        this.matchingMapper = matchingMapper;
        this.outboxService = outboxService;
    }

    // -------------------------------------------------------------------------
    // Priority Scoring & Ingestion
    // -------------------------------------------------------------------------

    /**
     * Enrolls a verified aid request into the matching pipeline and calculates its initial priority score.
     */
    @Transactional
    public MatchedRequest registerVerifiedRequest(
        UUID requestId,
        UUID receiverId,
        UUID disasterId,
        AidCategory category,
        BigDecimal quantity,
        String unit,
        UrgencyLevel urgencyLevel,
        int householdSize,
        DeliveryAddressDto address
    ) {
        log.info("Registering verified request for matching: requestId={}, category={}, urgency={}",
            requestId, category, urgencyLevel);

        double priorityScore = computePriorityScore(urgencyLevel, householdSize, Instant.now(), quantity, BigDecimal.ZERO);

        MatchedRequest matchedRequest = MatchedRequest.builder()
            .requestId(requestId)
            .receiverId(receiverId)
            .disasterId(disasterId)
            .category(category)
            .quantityRequested(quantity)
            .quantityAllocated(BigDecimal.ZERO)
            .unit(unit != null ? unit : "units")
            .urgencyLevel(urgencyLevel != null ? urgencyLevel : UrgencyLevel.MEDIUM)
            .householdSize(householdSize > 0 ? householdSize : 1)
            .deliveryStreet(address != null ? address.street() : null)
            .deliveryCity(address != null ? address.city() : null)
            .deliveryState(address != null ? address.state() : null)
            .deliveryCountry(address != null ? address.country() : null)
            .deliveryZip(address != null ? address.zip() : null)
            .priorityScore(priorityScore)
            .status("PENDING")
            .verifiedAt(Instant.now())
            .build();

        return matchedRequestRepository.save(matchedRequest);
    }

    /**
     * Calculates composite priority score based on urgency, household size, wait time, and shortage.
     */
    public double computePriorityScore(
        UrgencyLevel urgency,
        int householdSize,
        Instant verifiedAt,
        BigDecimal requestedQty,
        BigDecimal availableQty
    ) {
        double urgencyWeight = switch (urgency != null ? urgency : UrgencyLevel.MEDIUM) {
            case LOW -> 10.0;
            case MEDIUM -> 20.0;
            case HIGH -> 35.0;
            case CRITICAL -> 50.0;
        };

        double populationWeight = Math.min(Math.max(householdSize, 1), 10) * 3.0;

        long hoursWaiting = verifiedAt != null
            ? Math.max(0, Duration.between(verifiedAt, Instant.now()).toHours())
            : 0;
        double waitTimeWeight = Math.min(hoursWaiting * 0.5, 20.0);

        double shortageWeight = 0.0;
        if (availableQty != null && requestedQty != null && availableQty.compareTo(BigDecimal.ZERO) > 0) {
            if (availableQty.compareTo(requestedQty) < 0) {
                shortageWeight = 10.0;
            }
        }

        return urgencyWeight + populationWeight + waitTimeWeight + shortageWeight;
    }

    // -------------------------------------------------------------------------
    // Suggestions & Recommendations
    // -------------------------------------------------------------------------

    /**
     * Generates candidate allocation suggestions for a request by scoring available items from inventory.
     */
    @Transactional(readOnly = true)
    public List<AllocationSuggestionResponse> generateSuggestions(UUID requestId) {
        log.info("Generating allocation suggestions for requestId={}", requestId);

        MatchedRequest request = matchedRequestRepository.findById(requestId).orElse(null);
        AidCategory category = request != null ? request.getCategory() : null;
        BigDecimal requestedQty = request != null ? request.getQuantityRequested() : BigDecimal.ONE;

        // Fetch candidate inventory items
        Page<ItemClientDto> itemsPage = inventoryClient.getItems(category, null, PageRequest.of(0, 50));
        List<ItemClientDto> candidateItems = itemsPage != null ? itemsPage.getContent() : Collections.emptyList();

        List<AllocationSuggestionResponse> suggestions = new ArrayList<>();
        for (ItemClientDto item : candidateItems) {
            BigDecimal available = item.quantityAvailable() != null ? item.quantityAvailable() : BigDecimal.ZERO;
            if (available.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            // Calculate match score between 0.0 and 1.0
            double coverageRatio = Math.min(1.0, available.divide(requestedQty, 4, RoundingMode.HALF_UP).doubleValue());
            double score = Math.round(coverageRatio * 0.8 * 100.0) / 100.0 + 0.2; // Baseline 0.2 + up to 0.8 based on availability

            String rationale = available.compareTo(requestedQty) >= 0
                ? "Full demand satisfaction available in warehouse"
                : "Partial supply available (" + available + " of " + requestedQty + " requested) - proportional allocation recommended";

            suggestions.add(new AllocationSuggestionResponse(
                requestId,
                item.itemId(),
                item.warehouseId(),
                item.name(),
                available,
                Math.min(1.0, score),
                rationale
            ));
        }

        suggestions.sort(Comparator.comparingDouble(AllocationSuggestionResponse::matchScore).reversed());
        return suggestions;
    }

    // -------------------------------------------------------------------------
    // Allocations CRUD & Workflow
    // -------------------------------------------------------------------------

    /**
     * Creates a new allocation proposal in PENDING_APPROVAL status.
     */
    @Transactional
    public AllocationResponse createAllocation(CreateAllocationRequest request) {
        log.info("Creating allocation: requestId={}, itemId={}, quantity={}",
            request.requestId(), request.itemId(), request.quantity());

        ItemClientDto item = inventoryClient.getItem(request.itemId());
        if (item == null) {
            throw new InventoryUnavailableException("Item not found or inventory service unavailable: " + request.itemId());
        }

        BigDecimal available = item.quantityAvailable() != null ? item.quantityAvailable() : BigDecimal.ZERO;
        if (available.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InventoryUnavailableException("No stock currently available for item: " + item.name());
        }

        // Apply proportional cap if requested exceeds available
        BigDecimal allocQty = request.quantity();
        if (allocQty.compareTo(available) > 0) {
            log.warn("Requested quantity {} exceeds available stock {}. Applying proportional cap.", allocQty, available);
            allocQty = available;
        }

        MatchedRequest matchedReq = matchedRequestRepository.findById(request.requestId()).orElse(null);
        UUID receiverId = matchedReq != null ? matchedReq.getReceiverId() : UUID.randomUUID();
        UUID disasterId = matchedReq != null ? matchedReq.getDisasterId() : UUID.randomUUID();
        double priority = matchedReq != null ? matchedReq.getPriorityScore() : 50.0;

        Allocation allocation = Allocation.builder()
            .allocationId(UUID.randomUUID())
            .requestId(request.requestId())
            .receiverId(receiverId)
            .disasterId(disasterId)
            .itemId(request.itemId())
            .warehouseId(item.warehouseId())
            .quantity(allocQty)
            .unit(item.unit() != null ? item.unit() : "units")
            .status(AllocationStatus.PENDING_APPROVAL)
            .deliveryStreet(matchedReq != null ? matchedReq.getDeliveryStreet() : null)
            .deliveryCity(matchedReq != null ? matchedReq.getDeliveryCity() : null)
            .deliveryState(matchedReq != null ? matchedReq.getDeliveryState() : null)
            .deliveryCountry(matchedReq != null ? matchedReq.getDeliveryCountry() : null)
            .deliveryZip(matchedReq != null ? matchedReq.getDeliveryZip() : null)
            .priorityScore(priority)
            .notes(request.notes())
            .build();

        Allocation saved = allocationRepository.save(allocation);

        if (matchedReq != null) {
            matchedReq.setQuantityAllocated(matchedReq.getQuantityAllocated().add(allocQty));
            matchedReq.setStatus("ALLOCATING");
            matchedRequestRepository.save(matchedReq);
        }

        log.info("Allocation created in PENDING_APPROVAL: allocationId={}", saved.getAllocationId());
        return matchingMapper.toResponse(saved);
    }

    /**
     * Approves an allocation and enqueues AllocationApproved outbox event to trigger inventory reservation.
     */
    @Transactional
    public AllocationResponse approveAllocation(UUID allocationId, ApproveAllocationRequest request, String reviewer) {
        log.info("Approving allocation: allocationId={}, reviewer={}", allocationId, reviewer);
        Allocation allocation = findActiveAllocation(allocationId);

        if (allocation.getStatus() != AllocationStatus.PENDING_APPROVAL && allocation.getStatus() != AllocationStatus.SUGGESTED) {
            throw new InvalidAllocationStateException("Cannot approve allocation in status: " + allocation.getStatus());
        }

        String approver = (reviewer != null && !reviewer.isBlank()) ? reviewer : request.approvedBy();
        allocation.setStatus(AllocationStatus.APPROVED);
        allocation.setApprovedBy(approver);
        if (request.notes() != null) {
            allocation.setNotes(request.notes());
        }

        Allocation saved = allocationRepository.save(allocation);

        DeliveryAddressDto addr = new DeliveryAddressDto(
            saved.getDeliveryStreet(),
            saved.getDeliveryCity(),
            saved.getDeliveryState(),
            saved.getDeliveryCountry(),
            saved.getDeliveryZip()
        );

        // Publish AllocationApproved event via transactional outbox
        AllocationApprovedPayload payload = new AllocationApprovedPayload(
            saved.getAllocationId(),
            saved.getRequestId(),
            saved.getReceiverId(),
            saved.getDisasterId(),
            saved.getItemId(),
            saved.getWarehouseId(),
            saved.getQuantity(),
            saved.getUnit(),
            approver,
            addr
        );

        outboxService.enqueue(
            KafkaTopics.ALLOCATION_APPROVED,
            "ALLOCATION",
            saved.getAllocationId(),
            "AllocationApproved",
            payload,
            saved.getRequestId().toString()
        );

        log.info("Allocation approved and event enqueued: allocationId={}", saved.getAllocationId());
        return matchingMapper.toResponse(saved);
    }

    /**
     * Rejects an allocation and publishes AllocationRejected outbox event.
     */
    @Transactional
    public AllocationResponse rejectAllocation(UUID allocationId, RejectAllocationRequest request, String reviewer) {
        log.info("Rejecting allocation: allocationId={}, reviewer={}, reason={}",
            allocationId, reviewer, request.reason());
        Allocation allocation = findActiveAllocation(allocationId);

        if (allocation.getStatus() == AllocationStatus.REJECTED || allocation.getStatus() == AllocationStatus.RESERVATION_FAILED) {
            throw new InvalidAllocationStateException("Cannot reject allocation already in status: " + allocation.getStatus());
        }

        allocation.setStatus(AllocationStatus.REJECTED);
        allocation.setRejectionReason(request.reason());

        Allocation saved = allocationRepository.save(allocation);

        // Compensate / unlock matched request
        matchedRequestRepository.findById(saved.getRequestId()).ifPresent(req -> {
            req.setQuantityAllocated(req.getQuantityAllocated().subtract(saved.getQuantity()).max(BigDecimal.ZERO));
            req.setStatus("PENDING");
            matchedRequestRepository.save(req);
        });

        // Publish AllocationRejected event via transactional outbox
        AllocationRejectedPayload payload = new AllocationRejectedPayload(
            saved.getAllocationId(),
            saved.getRequestId(),
            saved.getReceiverId(),
            request.reason(),
            reviewer != null ? reviewer : "coordinator"
        );

        outboxService.enqueue(
            KafkaTopics.ALLOCATION_REJECTED,
            "ALLOCATION",
            saved.getAllocationId(),
            "AllocationRejected",
            payload,
            saved.getRequestId().toString()
        );

        log.info("Allocation rejected and event enqueued: allocationId={}", saved.getAllocationId());
        return matchingMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public AllocationResponse getAllocation(UUID allocationId) {
        return matchingMapper.toResponse(findActiveAllocation(allocationId));
    }

    @Transactional(readOnly = true)
    public Page<AllocationResponse> listAllocations(AllocationStatus status, UUID disasterId, Pageable pageable) {
        Page<Allocation> page;
        if (status != null && disasterId != null) {
            page = allocationRepository.findAllByStatusAndDisasterId(status, disasterId, pageable);
        } else if (status != null) {
            page = allocationRepository.findAllByStatus(status, pageable);
        } else if (disasterId != null) {
            page = allocationRepository.findAllByDisasterId(disasterId, pageable);
        } else {
            page = allocationRepository.findAllActive(pageable);
        }
        return page.map(matchingMapper::toResponse);
    }

    // -------------------------------------------------------------------------
    // Saga Orchestration & Compensation
    // -------------------------------------------------------------------------

    /**
     * Saga step: Stock reserved successfully by Inventory Service.
     */
    @Transactional
    public void applyStockReserved(UUID allocationId, UUID reservationId) {
        log.info("Saga update: StockReserved received for allocationId={}, reservationId={}", allocationId, reservationId);
        allocationRepository.findActiveById(allocationId).ifPresent(alloc -> {
            log.info("Stock confirmed for allocation: allocationId={}", alloc.getAllocationId());
        });
    }

    /**
     * Saga compensation: Stock reservation failed by Inventory Service. Reverts allocation and unlocks request.
     */
    @Transactional
    public void applyStockReservationFailed(UUID allocationId, String reason) {
        log.warn("Saga compensation: StockReservationFailed for allocationId={}, reason={}", allocationId, reason);
        allocationRepository.findActiveById(allocationId).ifPresent(alloc -> {
            alloc.setStatus(AllocationStatus.RESERVATION_FAILED);
            alloc.setRejectionReason("Stock reservation failed: " + reason);
            allocationRepository.save(alloc);

            // Revert matched request back to PENDING so it can be re-evaluated
            matchedRequestRepository.findById(alloc.getRequestId()).ifPresent(req -> {
                req.setQuantityAllocated(req.getQuantityAllocated().subtract(alloc.getQuantity()).max(BigDecimal.ZERO));
                req.setStatus("PENDING");
                matchedRequestRepository.save(req);
                log.info("Reverted MatchedRequest {} back to PENDING after reservation failure", req.getRequestId());
            });
        });
    }

    /**
     * Saga update: Shipment assigned by Transport Service.
     */
    @Transactional
    public void applyShipmentAssigned(UUID allocationId) {
        log.info("Saga update: ShipmentAssigned for allocationId={}", allocationId);
    }

    /**
     * Saga completion: Shipment delivered to beneficiary.
     */
    @Transactional
    public void applyShipmentDelivered(UUID allocationId) {
        log.info("Saga update: ShipmentDelivered for allocationId={}", allocationId);
        allocationRepository.findActiveById(allocationId).ifPresent(alloc -> {
            matchedRequestRepository.findById(alloc.getRequestId()).ifPresent(req -> {
                req.setStatus("DELIVERED");
                matchedRequestRepository.save(req);
                log.info("MatchedRequest {} finalized as DELIVERED", req.getRequestId());
            });
        });
    }

    /**
     * Scheduled saga timeout detection: finds allocations lingering in APPROVED state without progression.
     */
    @Scheduled(fixedDelay = 3600000) // Hourly
    @Transactional
    public void processSagaTimeouts() {
        Instant cutoff = Instant.now().minus(Duration.ofHours(sagaTimeoutHours));
        List<Allocation> staleAllocations = allocationRepository.findStaleAllocations(AllocationStatus.APPROVED, cutoff);
        log.info("Running saga timeout check: found {} stale allocations exceeding {}h",
            staleAllocations.size(), sagaTimeoutHours);

        for (Allocation alloc : staleAllocations) {
            log.warn("Timing out stale allocation: allocationId={}", alloc.getAllocationId());
            alloc.setStatus(AllocationStatus.RESERVATION_FAILED);
            alloc.setRejectionReason("Saga timeout: no stock reservation confirmation within " + sagaTimeoutHours + "h");
            allocationRepository.save(alloc);

            matchedRequestRepository.findById(alloc.getRequestId()).ifPresent(req -> {
                req.setStatus("PENDING");
                matchedRequestRepository.save(req);
            });
        }
    }

    private Allocation findActiveAllocation(UUID allocationId) {
        return allocationRepository.findActiveById(allocationId)
            .orElseThrow(() -> new AllocationNotFoundException(allocationId));
    }
}
