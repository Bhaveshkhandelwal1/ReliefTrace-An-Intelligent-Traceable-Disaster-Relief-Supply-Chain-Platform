package com.disasterreliefnet.transport.event;

import com.disasterreliefnet.common.dto.DeliveryAddressDto;
import com.disasterreliefnet.common.dto.EventEnvelope;
import com.disasterreliefnet.common.enums.FailureReason;
import com.disasterreliefnet.common.event.payload.StockReservationFailedPayload;
import com.disasterreliefnet.common.event.payload.StockReservedPayload;
import com.disasterreliefnet.common.idempotency.IdempotentEventConsumer;
import com.disasterreliefnet.transport.service.ShipmentService;
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
class TransportEventConsumerTest {

    @Mock
    private ShipmentService shipmentService;

    @Mock
    private IdempotentEventConsumer idempotentEventConsumer;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private TransportEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new TransportEventConsumer(shipmentService, idempotentEventConsumer, objectMapper);
    }

    @Test
    @DisplayName("Should consume StockReserved event and trigger shipment creation")
    void handleStockReserved_Success() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID allocationId = UUID.randomUUID();
        StockReservedPayload payload = new StockReservedPayload(
            UUID.randomUUID(),
            allocationId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            BigDecimal.valueOf(50),
            "KITS",
            new DeliveryAddressDto("Depot A", "Dallas", "TX", "US", "75001"),
            new DeliveryAddressDto("Shelter B", "Houston", "TX", "US", "77001")
        );

        EventEnvelope<StockReservedPayload> envelope = new EventEnvelope<>(
            eventId,
            "StockReserved",
            "RESERVATION",
            payload.reservationId().toString(),
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
        }).when(idempotentEventConsumer).execute(eq(eventId), eq("StockReserved"), any(Runnable.class));

        consumer.handleStockReserved(json);

        verify(idempotentEventConsumer).execute(eq(eventId), eq("StockReserved"), any(Runnable.class));
        verify(shipmentService).createShipmentFromStockReserved(payload);
    }

    @Test
    @DisplayName("Should consume StockReservationFailed event and cancel shipment")
    void handleStockReservationFailed_Success() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID allocationId = UUID.randomUUID();
        StockReservationFailedPayload payload = new StockReservationFailedPayload(
            allocationId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            BigDecimal.valueOf(100),
            BigDecimal.valueOf(20),
            FailureReason.INSUFFICIENT_STOCK
        );

        EventEnvelope<StockReservationFailedPayload> envelope = new EventEnvelope<>(
            eventId,
            "StockReservationFailed",
            "RESERVATION",
            allocationId.toString(),
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
        }).when(idempotentEventConsumer).execute(eq(eventId), eq("StockReservationFailed"), any(Runnable.class));

        consumer.handleStockReservationFailed(json);

        verify(idempotentEventConsumer).execute(eq(eventId), eq("StockReservationFailed"), any(Runnable.class));
        verify(shipmentService).cancelShipmentByAllocation(eq(allocationId), contains("INSUFFICIENT_STOCK"));
    }
}
