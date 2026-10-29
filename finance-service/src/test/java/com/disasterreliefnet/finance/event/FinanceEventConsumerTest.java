package com.disasterreliefnet.finance.event;

import com.disasterreliefnet.common.dto.DeliveryAddressDto;
import com.disasterreliefnet.common.dto.EventEnvelope;
import com.disasterreliefnet.common.enums.DonationType;
import com.disasterreliefnet.common.event.payload.AllocationApprovedPayload;
import com.disasterreliefnet.common.event.payload.DonationRegisteredPayload;
import com.disasterreliefnet.common.idempotency.IdempotentEventConsumer;
import com.disasterreliefnet.finance.service.DisbursementService;
import com.disasterreliefnet.finance.service.PaymentService;
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
class FinanceEventConsumerTest {

    @Mock
    private PaymentService paymentService;

    @Mock
    private DisbursementService disbursementService;

    @Mock
    private IdempotentEventConsumer idempotentEventConsumer;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private FinanceEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new FinanceEventConsumer(
            paymentService,
            disbursementService,
            idempotentEventConsumer,
            objectMapper
        );
    }

    @Test
    @DisplayName("Should consume DonationRegistered event and create payment record")
    void handleDonationRegistered_Success() throws Exception {
        UUID eventId = UUID.randomUUID();
        DonationRegisteredPayload payload = new DonationRegisteredPayload(
            UUID.randomUUID(),
            UUID.randomUUID(),
            DonationType.MONETARY,
            BigDecimal.valueOf(500.00),
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
        verify(paymentService).createPaymentRecordFromDonation(payload);
    }

    @Test
    @DisplayName("Should consume AllocationApproved event and create disbursement")
    void handleAllocationApproved_Success() throws Exception {
        UUID eventId = UUID.randomUUID();
        AllocationApprovedPayload payload = new AllocationApprovedPayload(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            BigDecimal.valueOf(100),
            "UNITS",
            "approver-1",
            new DeliveryAddressDto("100 River St", "Austin", "TX", "US", "78701")
        );

        EventEnvelope<AllocationApprovedPayload> envelope = new EventEnvelope<>(
            eventId,
            "AllocationApproved",
            "ALLOCATION",
            payload.allocationId().toString(),
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
        }).when(idempotentEventConsumer).execute(eq(eventId), eq("AllocationApproved"), any(Runnable.class));

        consumer.handleAllocationApproved(json);

        verify(idempotentEventConsumer).execute(eq(eventId), eq("AllocationApproved"), any(Runnable.class));
        verify(disbursementService).createDisbursementFromAllocation(payload);
    }
}
