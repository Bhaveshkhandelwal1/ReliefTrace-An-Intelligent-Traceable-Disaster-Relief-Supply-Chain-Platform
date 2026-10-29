package com.disasterreliefnet.transport.event;

import com.disasterreliefnet.common.dto.EventEnvelope;
import com.disasterreliefnet.common.event.KafkaTopics;
import com.disasterreliefnet.common.event.payload.StockReservationFailedPayload;
import com.disasterreliefnet.common.event.payload.StockReservedPayload;
import com.disasterreliefnet.common.idempotency.IdempotentEventConsumer;
import com.disasterreliefnet.transport.service.ShipmentService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Idempotent Kafka consumer listening for stock reservations to trigger shipment creation,
 * or reservation failures to cancel pending shipments.
 */
@Component
public class TransportEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(TransportEventConsumer.class);

    private final ShipmentService shipmentService;
    private final IdempotentEventConsumer idempotentEventConsumer;
    private final ObjectMapper objectMapper;

    public TransportEventConsumer(
        ShipmentService shipmentService,
        IdempotentEventConsumer idempotentEventConsumer,
        ObjectMapper objectMapper
    ) {
        this.shipmentService = shipmentService;
        this.idempotentEventConsumer = idempotentEventConsumer;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = KafkaTopics.STOCK_RESERVED, groupId = "${spring.kafka.consumer.group-id:transport-service-group}")
    public void handleStockReserved(String message) {
        try {
            EventEnvelope<StockReservedPayload> envelope = objectMapper.readValue(
                message,
                new TypeReference<>() {}
            );

            idempotentEventConsumer.execute(envelope.eventId(), envelope.eventType(), () -> {
                log.info("Handling StockReserved event {} for allocation {}", envelope.eventId(), envelope.payload().allocationId());
                shipmentService.createShipmentFromStockReserved(envelope.payload());
            });
        } catch (Exception ex) {
            log.error("Failed to process StockReserved event: {}", ex.getMessage(), ex);
            throw new IllegalArgumentException("Invalid StockReserved event payload", ex);
        }
    }

    @KafkaListener(topics = KafkaTopics.STOCK_RESERVATION_FAILED, groupId = "${spring.kafka.consumer.group-id:transport-service-group}")
    public void handleStockReservationFailed(String message) {
        try {
            EventEnvelope<StockReservationFailedPayload> envelope = objectMapper.readValue(
                message,
                new TypeReference<>() {}
            );

            idempotentEventConsumer.execute(envelope.eventId(), envelope.eventType(), () -> {
                log.info("Handling StockReservationFailed event {} for allocation {}", envelope.eventId(), envelope.payload().allocationId());
                shipmentService.cancelShipmentByAllocation(
                    envelope.payload().allocationId(),
                    "Stock reservation failed: " + envelope.payload().failureReason()
                );
            });
        } catch (Exception ex) {
            log.error("Failed to process StockReservationFailed event: {}", ex.getMessage(), ex);
            throw new IllegalArgumentException("Invalid StockReservationFailed event payload", ex);
        }
    }

    @KafkaListener(topics = KafkaTopics.ROAD_CLOSED, groupId = "${spring.kafka.consumer.group-id:transport-service-group}")
    public void handleRoadClosed(String message) {
        try {
            EventEnvelope<com.disasterreliefnet.common.event.payload.RoadClosedPayload> envelope = objectMapper.readValue(
                message,
                new TypeReference<>() {}
            );

            idempotentEventConsumer.execute(envelope.eventId(), envelope.eventType(), () -> {
                log.info("Handling RoadClosed event {} for road {}", envelope.eventId(), envelope.payload().roadName());
                shipmentService.handleRoadClosure(envelope.payload());
            });
        } catch (Exception ex) {
            log.error("Failed to process RoadClosed event: {}", ex.getMessage(), ex);
            throw new IllegalArgumentException("Invalid RoadClosed event payload", ex);
        }
    }
}
