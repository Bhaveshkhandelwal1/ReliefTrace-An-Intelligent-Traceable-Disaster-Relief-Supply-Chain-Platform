package com.disasterreliefnet.analytics.event;

import com.disasterreliefnet.analytics.service.AnalyticsFactService;
import com.disasterreliefnet.common.dto.AffectedAreaDto;
import com.disasterreliefnet.common.dto.DeliveryAddressDto;
import com.disasterreliefnet.common.dto.EventEnvelope;
import com.disasterreliefnet.common.enums.AidCategory;
import com.disasterreliefnet.common.enums.DisasterType;
import com.disasterreliefnet.common.enums.DonationType;
import com.disasterreliefnet.common.enums.SeverityLevel;
import com.disasterreliefnet.common.enums.UrgencyLevel;
import com.disasterreliefnet.common.event.payload.DisasterDeclaredPayload;
import com.disasterreliefnet.common.event.payload.DonationRegisteredPayload;
import com.disasterreliefnet.common.event.payload.RequestCreatedPayload;
import com.disasterreliefnet.common.idempotency.IdempotentEventConsumer;
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
class AnalyticsEventConsumerTest {

    @Mock
    private AnalyticsFactService factService;

    @Mock
    private IdempotentEventConsumer idempotentEventConsumer;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private AnalyticsEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new AnalyticsEventConsumer(factService, idempotentEventConsumer, objectMapper);
    }

    @Test
    @DisplayName("Should consume DonationRegistered and delegate to fact service")
    void handleDonationRegistered_Success() throws Exception {
        UUID eventId = UUID.randomUUID();
        DonationRegisteredPayload payload = new DonationRegisteredPayload(
            UUID.randomUUID(),
            UUID.randomUUID(),
            DonationType.MONETARY,
            BigDecimal.valueOf(250.00),
            "USD",
            null,
            null,
            UUID.randomUUID(),
            "donor@relief.org",
            "Generous Donor"
        );

        EventEnvelope<DonationRegisteredPayload> envelope = new EventEnvelope<>(
            eventId,
            "DonationRegistered",
            "DONATION",
            payload.donationId().toString(),
            Instant.now(),
            1,
            "corr-1",
            payload
        );

        String json = objectMapper.writeValueAsString(envelope);

        doAnswer(invocation -> {
            Runnable action = invocation.getArgument(2);
            action.run();
            return null;
        }).when(idempotentEventConsumer).execute(eq(eventId), eq("DonationRegistered"), any(Runnable.class));

        consumer.handleDonationRegistered(json);

        verify(idempotentEventConsumer).execute(eq(eventId), eq("DonationRegistered"), any(Runnable.class));
        verify(factService).recordDonation(payload);
    }

    @Test
    @DisplayName("Should consume RequestCreated and delegate to fact service")
    void handleRequestCreated_Success() throws Exception {
        UUID eventId = UUID.randomUUID();
        RequestCreatedPayload payload = new RequestCreatedPayload(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            AidCategory.WATER,
            "Clean drinking water",
            BigDecimal.valueOf(100),
            "LITERS",
            UrgencyLevel.CRITICAL,
            new DeliveryAddressDto("1st Ave", "City", "ST", "US", "10001"),
            "receiver@relief.org",
            "Receiver"
        );

        EventEnvelope<RequestCreatedPayload> envelope = new EventEnvelope<>(
            eventId,
            "RequestCreated",
            "REQUEST",
            payload.requestId().toString(),
            Instant.now(),
            1,
            "corr-2",
            payload
        );

        String json = objectMapper.writeValueAsString(envelope);

        doAnswer(invocation -> {
            Runnable action = invocation.getArgument(2);
            action.run();
            return null;
        }).when(idempotentEventConsumer).execute(eq(eventId), eq("RequestCreated"), any(Runnable.class));

        consumer.handleRequestCreated(json);

        verify(idempotentEventConsumer).execute(eq(eventId), eq("RequestCreated"), any(Runnable.class));
        verify(factService).recordRequestCreated(payload);
    }

    @Test
    @DisplayName("Should consume DisasterDeclared and delegate to fact service")
    void handleDisasterDeclared_Success() throws Exception {
        UUID eventId = UUID.randomUUID();
        DisasterDeclaredPayload payload = new DisasterDeclaredPayload(
            UUID.randomUUID(),
            "Wildfire 2026",
            DisasterType.WILDFIRE,
            SeverityLevel.SEVERE,
            new AffectedAreaDto(40.58, -122.39, 50.0, "Northern Zone", "US"),
            Instant.now()
        );

        EventEnvelope<DisasterDeclaredPayload> envelope = new EventEnvelope<>(
            eventId,
            "DisasterDeclared",
            "DISASTER",
            payload.disasterId().toString(),
            Instant.now(),
            1,
            "corr-3",
            payload
        );

        String json = objectMapper.writeValueAsString(envelope);

        doAnswer(invocation -> {
            Runnable action = invocation.getArgument(2);
            action.run();
            return null;
        }).when(idempotentEventConsumer).execute(eq(eventId), eq("DisasterDeclared"), any(Runnable.class));

        consumer.handleDisasterDeclared(json);

        verify(idempotentEventConsumer).execute(eq(eventId), eq("DisasterDeclared"), any(Runnable.class));
        verify(factService).recordDisasterDeclared(payload);
    }
}
