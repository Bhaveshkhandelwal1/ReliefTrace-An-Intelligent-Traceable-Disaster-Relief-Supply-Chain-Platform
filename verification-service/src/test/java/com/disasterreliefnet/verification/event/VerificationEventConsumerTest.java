package com.disasterreliefnet.verification.event;

import com.disasterreliefnet.common.dto.DeliveryAddressDto;
import com.disasterreliefnet.common.dto.EventEnvelope;
import com.disasterreliefnet.common.enums.AidCategory;
import com.disasterreliefnet.common.enums.UrgencyLevel;
import com.disasterreliefnet.common.event.payload.RequestCreatedPayload;
import com.disasterreliefnet.common.idempotency.IdempotentEventConsumer;
import com.disasterreliefnet.verification.service.VerificationService;
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
class VerificationEventConsumerTest {

    @Mock
    private VerificationService verificationService;

    @Mock
    private IdempotentEventConsumer idempotentEventConsumer;

    private ObjectMapper objectMapper;
    private VerificationEventConsumer verificationEventConsumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        doAnswer(invocation -> {
            Runnable action = invocation.getArgument(2);
            action.run();
            return null;
        }).when(idempotentEventConsumer).execute(any(UUID.class), anyString(), any(Runnable.class));

        verificationEventConsumer = new VerificationEventConsumer(
            verificationService,
            idempotentEventConsumer,
            objectMapper
        );
    }

    @Test
    @DisplayName("handleRequestCreated should parse payload and invoke openCase")
    void shouldHandleRequestCreated() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        UUID disasterId = UUID.randomUUID();
        DeliveryAddressDto addr = new DeliveryAddressDto("12 Flood St", "Metro", "State", "US", "12345");

        RequestCreatedPayload payload = new RequestCreatedPayload(
            requestId,
            receiverId,
            disasterId,
            AidCategory.MEDICINE,
            "Urgent first aid kits",
            new BigDecimal("10.000"),
            "kits",
            UrgencyLevel.CRITICAL,
            addr,
            "victim@example.com",
            "Jane Doe"
        );

        EventEnvelope<RequestCreatedPayload> envelope = new EventEnvelope<>(
            eventId,
            "RequestCreated",
            "AID_REQUEST",
            requestId.toString(),
            Instant.now(),
            1,
            UUID.randomUUID().toString(),
            payload
        );

        String json = objectMapper.writeValueAsString(envelope);

        verificationEventConsumer.handleRequestCreated(json);

        verify(idempotentEventConsumer).execute(eq(eventId), eq("RequestCreated"), any(Runnable.class));
        verify(verificationService).openCase(requestId, receiverId);
    }
}
