package com.disasterreliefnet.donor.event;

import com.disasterreliefnet.common.dto.EventEnvelope;
import com.disasterreliefnet.common.enums.DonationStatus;
import com.disasterreliefnet.common.event.payload.PaymentCompletedPayload;
import com.disasterreliefnet.common.event.payload.ShipmentDeliveredPayload;
import com.disasterreliefnet.common.idempotency.IdempotentEventConsumer;
import com.disasterreliefnet.donor.domain.Donor;
import com.disasterreliefnet.donor.repository.DonorRepository;
import com.disasterreliefnet.donor.service.DonationService;
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
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DonorEventConsumerTest {

    @Mock
    private DonationService donationService;

    @Mock
    private DonorRepository donorRepository;

    @Mock
    private IdempotentEventConsumer idempotentEventConsumer;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private DonorEventConsumer donorEventConsumer;

    @BeforeEach
    void setUp() {
        donorEventConsumer = new DonorEventConsumer(
            donationService,
            donorRepository,
            idempotentEventConsumer,
            objectMapper
        );

        // Make idempotent runner execute the lambda directly in tests
        doAnswer(invocation -> {
            Runnable action = invocation.getArgument(2);
            action.run();
            return null;
        }).when(idempotentEventConsumer).execute(any(UUID.class), anyString(), any(Runnable.class));
    }

    @Test
    @DisplayName("Should handle PaymentCompleted and mark donation as PAID")
    void shouldHandlePaymentCompleted() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID donationId = UUID.randomUUID();
        UUID donorId = UUID.randomUUID();

        PaymentCompletedPayload payload = new PaymentCompletedPayload(
            UUID.randomUUID(),
            donationId,
            donorId,
            new BigDecimal("200.00"),
            "USD",
            "STRIPE-CH-1234",
            Instant.now()
        );

        EventEnvelope<PaymentCompletedPayload> envelope = new EventEnvelope<>(
            eventId,
            "PaymentCompleted",
            "PAYMENT",
            payload.paymentId().toString(),
            Instant.now(),
            1,
            "corr-1",
            payload
        );

        String message = objectMapper.writeValueAsString(envelope);

        donorEventConsumer.handlePaymentCompleted(message);

        verify(idempotentEventConsumer).execute(eq(eventId), eq("PaymentCompleted"), any(Runnable.class));
        verify(donationService).transitionDonationStatus(
            eq(donationId),
            eq(DonationStatus.PAID),
            contains("STRIPE-CH-1234")
        );
    }

    @Test
    @DisplayName("Should handle ShipmentDelivered and increment donor impact count")
    void shouldHandleShipmentDelivered() throws Exception {
        UUID eventId = UUID.randomUUID();
        ShipmentDeliveredPayload payload = new ShipmentDeliveredPayload(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            Instant.now(),
            "https://cdn.example.com/pod.jpg"
        );

        EventEnvelope<ShipmentDeliveredPayload> envelope = new EventEnvelope<>(
            eventId,
            "ShipmentDelivered",
            "SHIPMENT",
            payload.shipmentId().toString(),
            Instant.now(),
            1,
            "corr-2",
            payload
        );

        Donor donor1 = Donor.builder().donorId(UUID.randomUUID()).impactDeliveriesCount(0L).build();
        Donor donor2 = Donor.builder().donorId(UUID.randomUUID()).impactDeliveriesCount(5L).build();
        when(donorRepository.findAll()).thenReturn(List.of(donor1, donor2));

        String message = objectMapper.writeValueAsString(envelope);

        donorEventConsumer.handleShipmentDelivered(message);

        verify(idempotentEventConsumer).execute(eq(eventId), eq("ShipmentDelivered"), any(Runnable.class));
        assertEquals(1L, donor1.getImpactDeliveriesCount());
        assertEquals(6L, donor2.getImpactDeliveriesCount());
        verify(donorRepository).saveAll(List.of(donor1, donor2));
    }
}
