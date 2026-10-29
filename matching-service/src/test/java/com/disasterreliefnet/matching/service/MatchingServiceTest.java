package com.disasterreliefnet.matching.service;

import com.disasterreliefnet.common.dto.DeliveryAddressDto;
import com.disasterreliefnet.common.enums.AidCategory;
import com.disasterreliefnet.common.enums.AllocationStatus;
import com.disasterreliefnet.common.enums.UrgencyLevel;
import com.disasterreliefnet.common.event.KafkaTopics;
import com.disasterreliefnet.common.outbox.OutboxService;
import com.disasterreliefnet.matching.client.InventoryClient;
import com.disasterreliefnet.matching.client.ReceiverClient;
import com.disasterreliefnet.matching.client.dto.ItemClientDto;
import com.disasterreliefnet.matching.domain.Allocation;
import com.disasterreliefnet.matching.domain.MatchedRequest;
import com.disasterreliefnet.matching.dto.*;
import com.disasterreliefnet.matching.exception.AllocationNotFoundException;
import com.disasterreliefnet.matching.exception.InvalidAllocationStateException;
import com.disasterreliefnet.matching.exception.InventoryUnavailableException;
import com.disasterreliefnet.matching.mapper.MatchingMapper;
import com.disasterreliefnet.matching.mapper.MatchingMapperImpl;
import com.disasterreliefnet.matching.repository.AllocationRepository;
import com.disasterreliefnet.matching.repository.MatchedRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchingServiceTest {

    @Mock
    private AllocationRepository allocationRepository;

    @Mock
    private MatchedRequestRepository matchedRequestRepository;

    @Mock
    private InventoryClient inventoryClient;

    @Mock
    private ReceiverClient receiverClient;

    @Mock
    private OutboxService outboxService;

    private final MatchingMapper matchingMapper = new MatchingMapperImpl();

    private MatchingService matchingService;

    private UUID requestId;
    private UUID receiverId;
    private UUID disasterId;
    private UUID itemId;
    private UUID warehouseId;
    private UUID allocationId;
    private MatchedRequest sampleRequest;
    private Allocation sampleAllocation;

    @BeforeEach
    void setUp() {
        matchingService = new MatchingService(
            allocationRepository,
            matchedRequestRepository,
            inventoryClient,
            receiverClient,
            matchingMapper,
            outboxService
        );

        ReflectionTestUtils.setField(matchingService, "sagaTimeoutHours", 12);
        ReflectionTestUtils.setField(matchingService, "proportionalThreshold", 0.8);

        requestId = UUID.randomUUID();
        receiverId = UUID.randomUUID();
        disasterId = UUID.randomUUID();
        itemId = UUID.randomUUID();
        warehouseId = UUID.randomUUID();
        allocationId = UUID.randomUUID();

        sampleRequest = MatchedRequest.builder()
            .requestId(requestId)
            .receiverId(receiverId)
            .disasterId(disasterId)
            .category(AidCategory.FOOD)
            .quantityRequested(new BigDecimal("50.000"))
            .quantityAllocated(BigDecimal.ZERO)
            .unit("boxes")
            .urgencyLevel(UrgencyLevel.CRITICAL)
            .householdSize(5)
            .priorityScore(75.0)
            .status("PENDING")
            .verifiedAt(Instant.now())
            .build();

        sampleAllocation = Allocation.builder()
            .allocationId(allocationId)
            .requestId(requestId)
            .receiverId(receiverId)
            .disasterId(disasterId)
            .itemId(itemId)
            .warehouseId(warehouseId)
            .quantity(new BigDecimal("50.000"))
            .unit("boxes")
            .status(AllocationStatus.PENDING_APPROVAL)
            .priorityScore(75.0)
            .deliveryStreet("10 Market St")
            .deliveryCity("Flood City")
            .deliveryState("CA")
            .deliveryCountry("US")
            .deliveryZip("94103")
            .build();
    }

    @Test
    @DisplayName("registerVerifiedRequest should calculate priority score and save request")
    void shouldRegisterVerifiedRequest() {
        when(matchedRequestRepository.save(any(MatchedRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        MatchedRequest registered = matchingService.registerVerifiedRequest(
            requestId,
            receiverId,
            disasterId,
            AidCategory.WATER,
            new BigDecimal("100.000"),
            "litres",
            UrgencyLevel.HIGH,
            4,
            new DeliveryAddressDto("10 Flood Way", "Rivertown", "North", "US", "98001")
        );

        assertNotNull(registered);
        assertEquals(requestId, registered.getRequestId());
        assertEquals(AidCategory.WATER, registered.getCategory());
        assertTrue(registered.getPriorityScore() > 0.0);
        verify(matchedRequestRepository).save(any(MatchedRequest.class));
    }

    @Test
    @DisplayName("computePriorityScore should weigh urgency, population, wait time and shortage correctly")
    void shouldComputePriorityScoreWithWeights() {
        double criticalScore = matchingService.computePriorityScore(
            UrgencyLevel.CRITICAL, 6, Instant.now().minusSeconds(7200), new BigDecimal("100"), new BigDecimal("50")
        );
        double lowScore = matchingService.computePriorityScore(
            UrgencyLevel.LOW, 1, Instant.now(), new BigDecimal("10"), new BigDecimal("100")
        );

        assertTrue(criticalScore > lowScore, "Critical urgency with shortage must score significantly higher than low");
        assertEquals(13.0, lowScore, 0.01); // 10.0 (low) + 3.0 (pop 1) + 0 (wait) + 0 (shortage)
    }

    @Test
    @DisplayName("generateSuggestions should query inventory and produce scored recommendations")
    void shouldGenerateSuggestions() {
        when(matchedRequestRepository.findById(requestId)).thenReturn(Optional.of(sampleRequest));

        ItemClientDto item1 = new ItemClientDto(
            itemId, warehouseId, "Family Ration Pack", AidCategory.FOOD, "boxes",
            new BigDecimal("200.000"), BigDecimal.ZERO, new BigDecimal("200.000")
        );

        when(inventoryClient.getItems(eq(AidCategory.FOOD), isNull(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(item1)));

        List<AllocationSuggestionResponse> suggestions = matchingService.generateSuggestions(requestId);

        assertNotNull(suggestions);
        assertEquals(1, suggestions.size());
        assertEquals(itemId, suggestions.get(0).itemId());
        assertEquals("Family Ration Pack", suggestions.get(0).itemName());
        assertEquals(1.0, suggestions.get(0).matchScore());
    }

    @Test
    @DisplayName("createAllocation should validate stock and create allocation proposal")
    void shouldCreateAllocationProposal() {
        ItemClientDto item = new ItemClientDto(
            itemId, warehouseId, "Family Ration Pack", AidCategory.FOOD, "boxes",
            new BigDecimal("100.000"), BigDecimal.ZERO, new BigDecimal("100.000")
        );

        when(inventoryClient.getItem(itemId)).thenReturn(item);
        when(matchedRequestRepository.findById(requestId)).thenReturn(Optional.of(sampleRequest));
        when(allocationRepository.save(any(Allocation.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateAllocationRequest request = new CreateAllocationRequest(requestId, itemId, new BigDecimal("40.000"), "Urgent family needs");

        AllocationResponse response = matchingService.createAllocation(request);

        assertNotNull(response);
        assertEquals(requestId, response.requestId());
        assertEquals(itemId, response.itemId());
        assertEquals(AllocationStatus.PENDING_APPROVAL, response.status());
        assertEquals(new BigDecimal("40.000"), response.quantity());
    }

    @Test
    @DisplayName("createAllocation should cap quantity proportionally when request exceeds stock")
    void shouldApplyProportionalCapWhenStockInsufficient() {
        ItemClientDto item = new ItemClientDto(
            itemId, warehouseId, "Limited Medicine", AidCategory.MEDICINE, "kits",
            new BigDecimal("25.000"), BigDecimal.ZERO, new BigDecimal("25.000")
        );

        when(inventoryClient.getItem(itemId)).thenReturn(item);
        when(matchedRequestRepository.findById(requestId)).thenReturn(Optional.of(sampleRequest));
        when(allocationRepository.save(any(Allocation.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateAllocationRequest request = new CreateAllocationRequest(requestId, itemId, new BigDecimal("60.000"), "Shortage allocation");

        AllocationResponse response = matchingService.createAllocation(request);

        assertEquals(new BigDecimal("25.000"), response.quantity(), "Quantity should be capped to available 25.000");
    }

    @Test
    @DisplayName("createAllocation should throw InventoryUnavailableException when stock is zero")
    void shouldThrowWhenNoStockAvailable() {
        ItemClientDto item = new ItemClientDto(
            itemId, warehouseId, "Out of Stock Food", AidCategory.FOOD, "boxes",
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
        );

        when(inventoryClient.getItem(itemId)).thenReturn(item);

        CreateAllocationRequest request = new CreateAllocationRequest(requestId, itemId, new BigDecimal("10.000"), null);

        assertThrows(InventoryUnavailableException.class, () -> matchingService.createAllocation(request));
    }

    @Test
    @DisplayName("approveAllocation should transition status to APPROVED and publish outbox event")
    void shouldApproveAllocationAndPublishEvent() {
        when(allocationRepository.findActiveById(allocationId)).thenReturn(Optional.of(sampleAllocation));
        when(allocationRepository.save(any(Allocation.class))).thenAnswer(inv -> inv.getArgument(0));

        ApproveAllocationRequest request = new ApproveAllocationRequest("coordinator-1", "Approved for dispatch");
        AllocationResponse response = matchingService.approveAllocation(allocationId, request, "coordinator-1");

        assertNotNull(response);
        assertEquals(AllocationStatus.APPROVED, response.status());
        assertEquals("coordinator-1", response.approvedBy());

        verify(outboxService).enqueue(
            eq(KafkaTopics.ALLOCATION_APPROVED),
            eq("ALLOCATION"),
            eq(allocationId),
            eq("AllocationApproved"),
            any(),
            eq(requestId.toString())
        );
    }

    @Test
    @DisplayName("rejectAllocation should transition status to REJECTED, compensate MatchedRequest, and publish event")
    void shouldRejectAllocationAndCompensate() {
        when(allocationRepository.findActiveById(allocationId)).thenReturn(Optional.of(sampleAllocation));
        when(allocationRepository.save(any(Allocation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(matchedRequestRepository.findById(requestId)).thenReturn(Optional.of(sampleRequest));

        sampleRequest.setQuantityAllocated(new BigDecimal("50.000"));

        RejectAllocationRequest request = new RejectAllocationRequest("Duplicate dispatch avoided");
        AllocationResponse response = matchingService.rejectAllocation(allocationId, request, "coordinator-jane");

        assertNotNull(response);
        assertEquals(AllocationStatus.REJECTED, response.status());
        assertEquals("Duplicate dispatch avoided", response.rejectionReason());

        // Verify matched request unlocked
        assertEquals(0, sampleRequest.getQuantityAllocated().compareTo(BigDecimal.ZERO));
        assertEquals("PENDING", sampleRequest.getStatus());

        verify(outboxService).enqueue(
            eq(KafkaTopics.ALLOCATION_REJECTED),
            eq("ALLOCATION"),
            eq(allocationId),
            eq("AllocationRejected"),
            any(),
            eq(requestId.toString())
        );
    }

    @Test
    @DisplayName("applyStockReservationFailed should trigger saga compensation and revert request to PENDING")
    void shouldCompensateOnStockReservationFailed() {
        when(allocationRepository.findActiveById(allocationId)).thenReturn(Optional.of(sampleAllocation));
        when(matchedRequestRepository.findById(requestId)).thenReturn(Optional.of(sampleRequest));

        sampleAllocation.setStatus(AllocationStatus.APPROVED);
        sampleRequest.setQuantityAllocated(new BigDecimal("50.000"));

        matchingService.applyStockReservationFailed(allocationId, "DAMAGED_ITEMS");

        assertEquals(AllocationStatus.RESERVATION_FAILED, sampleAllocation.getStatus());
        assertEquals("PENDING", sampleRequest.getStatus());
        assertEquals(0, sampleRequest.getQuantityAllocated().compareTo(BigDecimal.ZERO));
        verify(allocationRepository).save(sampleAllocation);
        verify(matchedRequestRepository).save(sampleRequest);
    }

    @Test
    @DisplayName("processSagaTimeouts should detect and compensate stale unconfirmed allocations")
    void shouldCompensateStaleAllocationsOnTimeout() {
        when(allocationRepository.findStaleAllocations(eq(AllocationStatus.APPROVED), any(Instant.class)))
            .thenReturn(List.of(sampleAllocation));
        when(matchedRequestRepository.findById(requestId)).thenReturn(Optional.of(sampleRequest));

        matchingService.processSagaTimeouts();

        assertEquals(AllocationStatus.RESERVATION_FAILED, sampleAllocation.getStatus());
        assertEquals("PENDING", sampleRequest.getStatus());
        verify(allocationRepository).save(sampleAllocation);
    }
}
