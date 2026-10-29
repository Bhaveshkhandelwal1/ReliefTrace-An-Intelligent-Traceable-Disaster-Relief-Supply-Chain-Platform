package com.disasterreliefnet.receiver.service;

import com.disasterreliefnet.common.dto.DeliveryAddressDto;
import com.disasterreliefnet.common.enums.AidCategory;
import com.disasterreliefnet.common.enums.RequestStatus;
import com.disasterreliefnet.common.enums.UrgencyLevel;
import com.disasterreliefnet.common.enums.VerificationStatus;
import com.disasterreliefnet.common.event.KafkaTopics;
import com.disasterreliefnet.common.outbox.OutboxService;
import com.disasterreliefnet.receiver.domain.AidRequest;
import com.disasterreliefnet.receiver.domain.Receiver;
import com.disasterreliefnet.receiver.dto.*;
import com.disasterreliefnet.receiver.exception.DuplicateRequestException;
import com.disasterreliefnet.receiver.exception.InvalidStatusTransitionException;
import com.disasterreliefnet.receiver.exception.ReceiverNotFoundException;
import com.disasterreliefnet.receiver.exception.RequestNotFoundException;
import com.disasterreliefnet.receiver.mapper.ReceiverMapper;
import com.disasterreliefnet.receiver.mapper.ReceiverMapperImpl;
import com.disasterreliefnet.receiver.repository.AidRequestRepository;
import com.disasterreliefnet.receiver.repository.ReceiverRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReceiverServiceTest {

    @Mock
    private ReceiverRepository receiverRepository;

    @Mock
    private AidRequestRepository aidRequestRepository;

    @Mock
    private OutboxService outboxService;

    private final ReceiverMapper receiverMapper = new ReceiverMapperImpl();

    private ReceiverService receiverService;

    private UUID receiverId;
    private UUID disasterId;
    private Receiver sampleReceiver;
    private DeliveryAddressDto sampleAddress;

    @BeforeEach
    void setUp() {
        receiverService = new ReceiverService(
            receiverRepository,
            aidRequestRepository,
            receiverMapper,
            outboxService
        );

        receiverId = UUID.randomUUID();
        disasterId = UUID.randomUUID();
        sampleAddress = new DeliveryAddressDto("123 Flood St", "Metro City", "Central", "US", "94101");

        sampleReceiver = Receiver.builder()
            .receiverId(receiverId)
            .firstName("John")
            .lastName("Doe")
            .email("john.doe@example.com")
            .phone("+15551234567")
            .disasterId(disasterId)
            .street(sampleAddress.street())
            .city(sampleAddress.city())
            .state(sampleAddress.state())
            .country(sampleAddress.country())
            .zip(sampleAddress.zip())
            .householdSize(4)
            .vulnerabilityTags("ELDERLY,CHILDREN")
            .verificationStatus(VerificationStatus.UNVERIFIED)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    }

    @Test
    @DisplayName("registerReceiver should create and return new receiver profile")
    void shouldRegisterReceiverSuccessfully() {
        CreateReceiverRequest request = new CreateReceiverRequest(
            "John",
            "Doe",
            "john.doe@example.com",
            "+15551234567",
            disasterId,
            sampleAddress,
            4,
            List.of("ELDERLY", "CHILDREN")
        );

        when(receiverRepository.save(any(Receiver.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReceiverResponse response = receiverService.registerReceiver(request);

        assertNotNull(response);
        assertEquals("John", response.firstName());
        assertEquals("Doe", response.lastName());
        assertEquals(VerificationStatus.UNVERIFIED, response.verificationStatus());
        assertEquals(4, response.householdSize());
        assertEquals(List.of("ELDERLY", "CHILDREN"), response.vulnerabilityTags());
        verify(receiverRepository).save(any(Receiver.class));
    }

    @Test
    @DisplayName("getReceiver should return receiver if active")
    void shouldGetActiveReceiver() {
        when(receiverRepository.findActiveById(receiverId)).thenReturn(Optional.of(sampleReceiver));

        ReceiverResponse response = receiverService.getReceiver(receiverId);

        assertNotNull(response);
        assertEquals(receiverId, response.receiverId());
    }

    @Test
    @DisplayName("getReceiver should throw ReceiverNotFoundException when not found")
    void shouldThrowWhenReceiverNotFound() {
        when(receiverRepository.findActiveById(receiverId)).thenReturn(Optional.empty());

        assertThrows(ReceiverNotFoundException.class, () -> receiverService.getReceiver(receiverId));
    }

    @Test
    @DisplayName("updateReceiver should update mutable fields")
    void shouldUpdateReceiver() {
        when(receiverRepository.findActiveById(receiverId)).thenReturn(Optional.of(sampleReceiver));
        when(receiverRepository.save(any(Receiver.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateReceiverRequest updateRequest = new UpdateReceiverRequest(
            "+15559876543",
            new DeliveryAddressDto("456 New St", "Shelterville", "North", "US", "94102"),
            5,
            List.of("DISABLED")
        );

        ReceiverResponse response = receiverService.updateReceiver(receiverId, updateRequest);

        assertEquals("+15559876543", response.phone());
        assertEquals(5, response.householdSize());
        assertEquals(List.of("DISABLED"), response.vulnerabilityTags());
        assertEquals("456 New St", response.currentLocation().street());
    }

    @Test
    @DisplayName("deleteReceiver should set deletedAt timestamp")
    void shouldSoftDeleteReceiver() {
        when(receiverRepository.findActiveById(receiverId)).thenReturn(Optional.of(sampleReceiver));

        receiverService.deleteReceiver(receiverId);

        assertNotNull(sampleReceiver.getDeletedAt());
        verify(receiverRepository).save(sampleReceiver);
    }

    @Test
    @DisplayName("listReceivers should return paginated active receivers")
    void shouldListReceivers() {
        Pageable pageable = PageRequest.of(0, 10);
        when(receiverRepository.findAllActive(pageable))
            .thenReturn(new PageImpl<>(List.of(sampleReceiver), pageable, 1));

        Page<ReceiverResponse> page = receiverService.listReceivers(null, pageable);

        assertEquals(1, page.getTotalElements());
        assertEquals(receiverId, page.getContent().get(0).receiverId());
    }

    @Test
    @DisplayName("createAidRequest should persist request and enqueue outbox event")
    void shouldCreateAidRequestAndEnqueueOutbox() {
        when(receiverRepository.findActiveById(receiverId)).thenReturn(Optional.of(sampleReceiver));
        when(aidRequestRepository.existsByDedupKeyAndDeletedAtIsNull(anyString())).thenReturn(false);
        when(aidRequestRepository.save(any(AidRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateAidRequestRequest req = new CreateAidRequestRequest(
            disasterId,
            AidCategory.FOOD,
            "Urgent family meal kits",
            new BigDecimal("10.000"),
            "boxes",
            UrgencyLevel.HIGH,
            sampleAddress
        );

        AidRequestResponse response = receiverService.createAidRequest(receiverId, req);

        assertNotNull(response);
        assertEquals(AidCategory.FOOD, response.category());
        assertEquals(RequestStatus.PENDING, response.status());
        assertEquals("boxes", response.unit());

        verify(outboxService).enqueue(
            eq(KafkaTopics.REQUEST_CREATED),
            eq("AID_REQUEST"),
            any(UUID.class),
            eq("RequestCreated"),
            any(),
            anyString()
        );
    }

    @Test
    @DisplayName("createAidRequest should throw DuplicateRequestException on duplicate key")
    void shouldThrowDuplicateRequestException() {
        when(receiverRepository.findActiveById(receiverId)).thenReturn(Optional.of(sampleReceiver));
        when(aidRequestRepository.existsByDedupKeyAndDeletedAtIsNull(anyString())).thenReturn(true);

        CreateAidRequestRequest req = new CreateAidRequestRequest(
            disasterId,
            AidCategory.FOOD,
            "Urgent food",
            new BigDecimal("10.000"),
            "boxes",
            UrgencyLevel.HIGH,
            sampleAddress
        );

        assertThrows(DuplicateRequestException.class, () -> receiverService.createAidRequest(receiverId, req));
        verify(aidRequestRepository, never()).save(any());
        verify(outboxService, never()).enqueue(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("cancelAidRequest should cancel pending request")
    void shouldCancelPendingAidRequest() {
        UUID requestId = UUID.randomUUID();
        AidRequest aidRequest = AidRequest.builder()
            .requestId(requestId)
            .receiver(sampleReceiver)
            .disasterId(disasterId)
            .category(AidCategory.WATER)
            .status(RequestStatus.PENDING)
            .quantityRequested(new BigDecimal("20.000"))
            .unit("litres")
            .urgencyLevel(UrgencyLevel.MEDIUM)
            .description("Clean water")
            .build();

        when(aidRequestRepository.findByRequestIdAndReceiverId(requestId, receiverId))
            .thenReturn(Optional.of(aidRequest));
        when(aidRequestRepository.save(any(AidRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AidRequestResponse response = receiverService.cancelAidRequest(receiverId, requestId);

        assertEquals(RequestStatus.CANCELLED, response.status());
        assertNotNull(aidRequest.getDeletedAt());
    }

    @Test
    @DisplayName("cancelAidRequest should throw InvalidStatusTransitionException if already allocated")
    void shouldThrowWhenCancellingAllocatedRequest() {
        UUID requestId = UUID.randomUUID();
        AidRequest aidRequest = AidRequest.builder()
            .requestId(requestId)
            .receiver(sampleReceiver)
            .disasterId(disasterId)
            .status(RequestStatus.ALLOCATED)
            .build();

        when(aidRequestRepository.findByRequestIdAndReceiverId(requestId, receiverId))
            .thenReturn(Optional.of(aidRequest));

        assertThrows(InvalidStatusTransitionException.class, () -> receiverService.cancelAidRequest(receiverId, requestId));
    }

    @Test
    @DisplayName("applyVerificationOutcome should update request status to VERIFIED when APPROVED")
    void shouldApplyApprovedVerificationOutcome() {
        UUID requestId = UUID.randomUUID();
        AidRequest aidRequest = AidRequest.builder()
            .requestId(requestId)
            .status(RequestStatus.PENDING)
            .build();

        when(aidRequestRepository.findById(requestId)).thenReturn(Optional.of(aidRequest));

        receiverService.applyVerificationOutcome(requestId, "APPROVED", "Identity confirmed");

        assertEquals(RequestStatus.VERIFIED, aidRequest.getStatus());
        verify(aidRequestRepository).save(aidRequest);
    }

    @Test
    @DisplayName("applyVerificationOutcome should update request status to REJECTED when REJECTED")
    void shouldApplyRejectedVerificationOutcome() {
        UUID requestId = UUID.randomUUID();
        AidRequest aidRequest = AidRequest.builder()
            .requestId(requestId)
            .status(RequestStatus.PENDING)
            .build();

        when(aidRequestRepository.findById(requestId)).thenReturn(Optional.of(aidRequest));

        receiverService.applyVerificationOutcome(requestId, "REJECTED", "Address invalid");

        assertEquals(RequestStatus.REJECTED, aidRequest.getStatus());
        verify(aidRequestRepository).save(aidRequest);
    }

    @Test
    @DisplayName("applyAllocationApproved should link allocationId and transition to ALLOCATED")
    void shouldApplyAllocationApproved() {
        UUID requestId = UUID.randomUUID();
        UUID allocationId = UUID.randomUUID();
        AidRequest aidRequest = AidRequest.builder()
            .requestId(requestId)
            .status(RequestStatus.VERIFIED)
            .build();

        when(aidRequestRepository.findById(requestId)).thenReturn(Optional.of(aidRequest));

        receiverService.applyAllocationApproved(requestId, allocationId);

        assertEquals(RequestStatus.ALLOCATED, aidRequest.getStatus());
        assertEquals(allocationId, aidRequest.getAllocationId());
        verify(aidRequestRepository).save(aidRequest);
    }

    @Test
    @DisplayName("applyShipmentDelivered should mark matching requests as DELIVERED")
    void shouldApplyShipmentDelivered() {
        UUID allocationId = UUID.randomUUID();
        AidRequest req1 = AidRequest.builder()
            .requestId(UUID.randomUUID())
            .allocationId(allocationId)
            .status(RequestStatus.IN_TRANSIT)
            .build();

        AidRequest req2 = AidRequest.builder()
            .requestId(UUID.randomUUID())
            .allocationId(UUID.randomUUID())
            .status(RequestStatus.IN_TRANSIT)
            .build();

        when(aidRequestRepository.findAllActive(Pageable.unpaged()))
            .thenReturn(new PageImpl<>(List.of(req1, req2)));

        receiverService.applyShipmentDelivered(allocationId);

        assertEquals(RequestStatus.DELIVERED, req1.getStatus());
        assertEquals(RequestStatus.IN_TRANSIT, req2.getStatus());
        verify(aidRequestRepository).save(req1);
        verify(aidRequestRepository, never()).save(req2);
    }
}
