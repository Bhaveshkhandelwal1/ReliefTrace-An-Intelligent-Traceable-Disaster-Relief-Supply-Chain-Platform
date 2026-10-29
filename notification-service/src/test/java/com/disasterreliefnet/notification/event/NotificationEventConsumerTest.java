package com.disasterreliefnet.notification.event;

import com.disasterreliefnet.common.dto.DeliveryAddressDto;
import com.disasterreliefnet.common.dto.EventEnvelope;
import com.disasterreliefnet.common.enums.DonationType;
import com.disasterreliefnet.common.event.payload.DonationRegisteredPayload;
import com.disasterreliefnet.common.event.payload.ShipmentAssignedPayload;
import com.disasterreliefnet.common.idempotency.IdempotentEventConsumer;
import com.disasterreliefnet.notification.service.NotificationService;
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
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationEventConsumerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private IdempotentEventConsumer idempotentEventConsumer;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private NotificationEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new NotificationEventConsumer(
            notificationService,
            idempotentEventConsumer,
            objectMapper
        );
    }

    @Test
    @DisplayName("Should consume DonationRegistered event and dispatch receipt notification to donor")
    void handleDonationRegistered_Success() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID donorId = UUID.randomUUID();
        DonationRegisteredPayload payload = new DonationRegisteredPayload(
            UUID.randomUUID(),
            donorId,
            DonationType.MONETARY,
            BigDecimal.valueOf(100.00),
            "USD",
            null,
            null,
            UUID.randomUUID(),
            "donor@relief.org",
            "Jane Donor"
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
        verify(notificationService).dispatch(
            eq(donorId.toString()),
            eq("Donation Received"),
            contains("Jane Donor"),
            eq(eventId),
            eq("DonationRegistered")
        );
    }

    @Test
    @DisplayName("Should consume ShipmentAssigned event and notify both receiver and driver")
    void handleShipmentAssigned_Success() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();

        ShipmentAssignedPayload payload = new ShipmentAssignedPayload(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            driverId,
            UUID.randomUUID(),
            receiverId,
            "Driver Dan",
            "TRK-9999",
            Instant.now(),
            new DeliveryAddressDto("Main St", "Houston", "TX", "US", "77001")
        );

        EventEnvelope<ShipmentAssignedPayload> envelope = new EventEnvelope<>(
            eventId,
            "ShipmentAssigned",
            "SHIPMENT",
            payload.shipmentId().toString(),
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
        }).when(idempotentEventConsumer).execute(eq(eventId), eq("ShipmentAssigned"), any(Runnable.class));

        consumer.handleShipmentAssigned(json);

        verify(idempotentEventConsumer).execute(eq(eventId), eq("ShipmentAssigned"), any(Runnable.class));
        verify(notificationService).dispatch(
            eq(receiverId.toString()),
            eq("Driver Dispatched"),
            contains("Driver Dan"),
            eq(eventId),
            eq("ShipmentAssigned")
        );
        verify(notificationService).dispatch(
            eq(driverId.toString()),
            eq("New Shipment Assignment"),
            contains("Houston"),
            eq(eventId),
            eq("ShipmentAssigned")
        );
    }
}
