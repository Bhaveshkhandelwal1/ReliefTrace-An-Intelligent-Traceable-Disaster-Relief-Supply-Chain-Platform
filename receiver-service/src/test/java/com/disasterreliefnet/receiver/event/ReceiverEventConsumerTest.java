package com.disasterreliefnet.receiver.event;

import com.disasterreliefnet.common.dto.DeliveryAddressDto;
import com.disasterreliefnet.common.dto.EventEnvelope;
import com.disasterreliefnet.common.event.payload.AllocationApprovedPayload;
import com.disasterreliefnet.common.event.payload.RequestVerifiedPayload;
import com.disasterreliefnet.common.event.payload.ShipmentDeliveredPayload;
import com.disasterreliefnet.common.idempotency.IdempotentEventConsumer;
import com.disasterreliefnet.receiver.service.ReceiverService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReceiverEventConsumerTest {

    @Mock
    private ReceiverService receiverService;

    @Mock
    private IdempotentEventConsumer idempotentEventConsumer;

    private ObjectMapper objectMapper;
    private ReceiverEventConsumer receiverEventConsumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // Configure the mock idempotent consumer to immediately execute the provided runnable
        doAnswer(invocation -> {
            Runnable action = invocation.getArgument(2);
            action.run();
            return null;
        }).when(idempotentEventConsumer).execute(any(UUID.class), anyString(), any(Runnable.class));

        receiverEventConsumer = new ReceiverEventConsumer(
            receiverService,
            idempotentEventConsumer,
            objectMapper
        );
    }

    @Test
    @DisplayName("handleRequestVerified should parse message and invoke applyVerificationOutcome")
    void shouldHandleRequestVerified() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        UUID caseId = UUID.randomUUID();

        RequestVerifiedPayload payload = new RequestVerifiedPayload(
            requestId,
            receiverId,
            caseId,
            "APPROVED",
            "coordinator-1",
            "Eligible beneficiary"
        );

        EventEnvelope<RequestVerifiedPayload> envelope = new EventEnvelope<>(
            eventId,
            "RequestVerified",
            "VERIFICATION_CASE",
            caseId.toString(),
            Instant.now(),
            1,
            UUID.randomUUID().toString(),
            payload
        );

        String json = objectMapper.writeValueAsString(envelope);

        receiverEventConsumer.handleRequestVerified(json);

        verify(idempotentEventConsumer).execute(eq(eventId), eq("RequestVerified"), any(Runnable.class));
        verify(receiverService).applyVerificationOutcome(requestId, "APPROVED", "Eligible beneficiary");
    }

    @Test
    @DisplayName("handleAllocationApproved should parse message and invoke applyAllocationApproved")
    void shouldHandleAllocationApproved() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID allocationId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        UUID disasterId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        UUID warehouseId = UUID.randomUUID();
        DeliveryAddressDto addr = new DeliveryAddressDto("12 Main", "City", "State", "US", "12345");

        AllocationApprovedPayload payload = new AllocationApprovedPayload(
            allocationId,
            requestId,
            receiverId,
            disasterId,
            itemId,
            warehouseId,
            new BigDecimal("10.00"),
            "boxes",
            "coordinator-2",
            addr
        );

        EventEnvelope<AllocationApprovedPayload> envelope = new EventEnvelope<>(
            eventId,
            "AllocationApproved",
            "ALLOCATION",
            allocationId.toString(),
            Instant.now(),
            1,
            UUID.randomUUID().toString(),
            payload
        );

        String json = objectMapper.writeValueAsString(envelope);

        receiverEventConsumer.handleAllocationApproved(json);

        verify(idempotentEventConsumer).execute(eq(eventId), eq("AllocationApproved"), any(Runnable.class));
        verify(receiverService).applyAllocationApproved(requestId, allocationId);
    }

    @Test
    @DisplayName("handleShipmentDelivered should parse message and invoke applyShipmentDelivered")
    void shouldHandleShipmentDelivered() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID shipmentId = UUID.randomUUID();
        UUID allocationId = UUID.randomUUID();
        UUID reservationId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();

        ShipmentDeliveredPayload payload = new ShipmentDeliveredPayload(
            shipmentId,
            allocationId,
            reservationId,
            receiverId,
            driverId,
            Instant.now(),
            "https://cdn.relief.org/pod/123.jpg"
        );

        EventEnvelope<ShipmentDeliveredPayload> envelope = new EventEnvelope<>(
            eventId,
            "ShipmentDelivered",
            "SHIPMENT",
            shipmentId.toString(),
            Instant.now(),
            1,
            UUID.randomUUID().toString(),
            payload
        );

        String json = objectMapper.writeValueAsString(envelope);

        receiverEventConsumer.handleShipmentDelivered(json);

        verify(idempotentEventConsumer).execute(eq(eventId), eq("ShipmentDelivered"), any(Runnable.class));
        verify(receiverService).applyShipmentDelivered(allocationId);
    }
}
