package com.disasterreliefnet.matching.event;

import com.disasterreliefnet.common.dto.DeliveryAddressDto;
import com.disasterreliefnet.common.dto.EventEnvelope;
import com.disasterreliefnet.common.enums.AidCategory;
import com.disasterreliefnet.common.enums.FailureReason;
import com.disasterreliefnet.common.enums.UrgencyLevel;
import com.disasterreliefnet.common.event.payload.*;
import com.disasterreliefnet.common.idempotency.IdempotentEventConsumer;
import com.disasterreliefnet.matching.client.ReceiverClient;
import com.disasterreliefnet.matching.client.dto.AidRequestClientDto;
import com.disasterreliefnet.matching.service.MatchingService;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchingEventConsumerTest {

    @Mock
    private MatchingService matchingService;

    @Mock
    private ReceiverClient receiverClient;

    @Mock
    private IdempotentEventConsumer idempotentEventConsumer;

    private ObjectMapper objectMapper;
    private MatchingEventConsumer matchingEventConsumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        doAnswer(invocation -> {
            Runnable action = invocation.getArgument(2);
            action.run();
            return null;
        }).when(idempotentEventConsumer).execute(any(UUID.class), anyString(), any(Runnable.class));

        matchingEventConsumer = new MatchingEventConsumer(
            matchingService,
            receiverClient,
            idempotentEventConsumer,
            objectMapper
        );
    }

    @Test
    @DisplayName("handleRequestVerified should register APPROVED request into matching pipeline")
    void shouldHandleRequestVerifiedAndRegister() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        UUID caseId = UUID.randomUUID();

        RequestVerifiedPayload payload = new RequestVerifiedPayload(
            requestId, receiverId, caseId, "APPROVED", "coord-1", "All documents valid"
        );

        EventEnvelope<RequestVerifiedPayload> envelope = new EventEnvelope<>(
            eventId, "RequestVerified", "VERIFICATION_CASE", caseId.toString(),
            Instant.now(), 1, UUID.randomUUID().toString(), payload
        );

        AidRequestClientDto aidReq = new AidRequestClientDto(
            requestId, receiverId, UUID.randomUUID(), AidCategory.FOOD,
            "Emergency food kits", new BigDecimal("50.000"), "boxes",
            UrgencyLevel.CRITICAL, new DeliveryAddressDto("10 Flood Rd", "Metropolis", "CA", "US", "94001"),
            null
        );

        when(receiverClient.getAidRequest(receiverId, requestId)).thenReturn(aidReq);

        matchingEventConsumer.handleRequestVerified(objectMapper.writeValueAsString(envelope));

        verify(matchingService).registerVerifiedRequest(
            eq(requestId), eq(receiverId), any(UUID.class), eq(AidCategory.FOOD),
            eq(new BigDecimal("50.000")), eq("boxes"), eq(UrgencyLevel.CRITICAL),
            eq(1), any()
        );
    }

    @Test
    @DisplayName("handleRequestVerified should skip REJECTED outcomes")
    void shouldSkipRejectedVerificationOutcome() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        UUID caseId = UUID.randomUUID();

        RequestVerifiedPayload payload = new RequestVerifiedPayload(
            requestId, receiverId, caseId, "REJECTED", "coord-1", "Invalid ID"
        );

        EventEnvelope<RequestVerifiedPayload> envelope = new EventEnvelope<>(
            eventId, "RequestVerified", "VERIFICATION_CASE", caseId.toString(),
            Instant.now(), 1, UUID.randomUUID().toString(), payload
        );

        matchingEventConsumer.handleRequestVerified(objectMapper.writeValueAsString(envelope));

        verifyNoInteractions(matchingService);
    }

    @Test
    @DisplayName("handleStockReservationFailed should invoke saga compensation")
    void shouldHandleStockReservationFailed() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID allocationId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        StockReservationFailedPayload payload = new StockReservationFailedPayload(
            allocationId, requestId, itemId, new BigDecimal("50.000"), new BigDecimal("0.000"),
            FailureReason.INSUFFICIENT_STOCK
        );

        EventEnvelope<StockReservationFailedPayload> envelope = new EventEnvelope<>(
            eventId, "StockReservationFailed", "RESERVATION", allocationId.toString(),
            Instant.now(), 1, UUID.randomUUID().toString(), payload
        );

        matchingEventConsumer.handleStockReservationFailed(objectMapper.writeValueAsString(envelope));

        verify(matchingService).applyStockReservationFailed(allocationId, "INSUFFICIENT_STOCK");
    }

    @Test
    @DisplayName("handleShipmentDelivered should finalize matching saga")
    void shouldHandleShipmentDelivered() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID allocationId = UUID.randomUUID();
        UUID shipmentId = UUID.randomUUID();
        UUID reservationId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();

        ShipmentDeliveredPayload payload = new ShipmentDeliveredPayload(
            shipmentId, allocationId, reservationId, receiverId, driverId,
            Instant.now(), "https://cdn.relief.org/pod/456.jpg"
        );

        EventEnvelope<ShipmentDeliveredPayload> envelope = new EventEnvelope<>(
            eventId, "ShipmentDelivered", "SHIPMENT", shipmentId.toString(),
            Instant.now(), 1, UUID.randomUUID().toString(), payload
        );

        matchingEventConsumer.handleShipmentDelivered(objectMapper.writeValueAsString(envelope));

        verify(matchingService).applyShipmentDelivered(allocationId);
    }
}
