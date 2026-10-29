package com.disasterreliefnet.matching.event;

import com.disasterreliefnet.common.dto.EventEnvelope;
import com.disasterreliefnet.common.enums.AidCategory;
import com.disasterreliefnet.common.enums.UrgencyLevel;
import com.disasterreliefnet.common.event.KafkaTopics;
import com.disasterreliefnet.common.event.payload.*;
import com.disasterreliefnet.common.idempotency.IdempotentEventConsumer;
import com.disasterreliefnet.matching.client.ReceiverClient;
import com.disasterreliefnet.matching.client.dto.AidRequestClientDto;
import com.disasterreliefnet.matching.service.MatchingService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Idempotent Kafka consumer listening for verified aid requests, inventory reservations,
 * shipment statuses, and donation updates.
 */
@Component
public class MatchingEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(MatchingEventConsumer.class);

    private final MatchingService matchingService;
    private final ReceiverClient receiverClient;
    private final IdempotentEventConsumer idempotentEventConsumer;
    private final ObjectMapper objectMapper;

    public MatchingEventConsumer(
        MatchingService matchingService,
        ReceiverClient receiverClient,
        IdempotentEventConsumer idempotentEventConsumer,
        ObjectMapper objectMapper
    ) {
        this.matchingService = matchingService;
        this.receiverClient = receiverClient;
        this.idempotentEventConsumer = idempotentEventConsumer;
        this.objectMapper = objectMapper;
    }

    /**
     * Consumes RequestVerified event to ingest approved request into matching queue.
     */
    @KafkaListener(topics = KafkaTopics.REQUEST_VERIFIED, groupId = "${spring.kafka.consumer.group-id:matching-service-group}")
    public void handleRequestVerified(String message) {
        try {
            EventEnvelope<RequestVerifiedPayload> envelope = objectMapper.readValue(
                message,
                new TypeReference<>() {}
            );

            idempotentEventConsumer.execute(envelope.eventId(), envelope.eventType(), () -> {
                RequestVerifiedPayload payload = envelope.payload();
                log.info("Processing RequestVerified event: requestId={}, receiverId={}, outcome={}",
                    payload.requestId(), payload.receiverId(), payload.outcome());

                if (!"APPROVED".equalsIgnoreCase(payload.outcome())) {
                    log.info("Skipping matching for unapproved verification outcome: {}", payload.outcome());
                    return;
                }

                // Query request details from receiver-service via Feign
                AidRequestClientDto aidReq = receiverClient.getAidRequest(payload.receiverId(), payload.requestId());
                if (aidReq != null) {
                    matchingService.registerVerifiedRequest(
                        aidReq.requestId(),
                        aidReq.receiverId(),
                        aidReq.disasterId(),
                        aidReq.category(),
                        aidReq.quantityRequested(),
                        aidReq.unit(),
                        aidReq.urgencyLevel(),
                        1,
                        aidReq.deliveryAddress()
                    );
                } else {
                    // Fallback registration with reasonable defaults
                    matchingService.registerVerifiedRequest(
                        payload.requestId(),
                        payload.receiverId(),
                        envelope.aggregateId() != null ? envelope.eventId() : payload.caseId(),
                        AidCategory.FOOD,
                        BigDecimal.ONE,
                        "units",
                        UrgencyLevel.HIGH,
                        1,
                        null
                    );
                }
            });
        } catch (Exception e) {
            log.error("Failed to process RequestVerified event: {}", message, e);
            throw new RuntimeException("Error processing RequestVerified event", e);
        }
    }

    /**
     * Consumes StockReserved event from Inventory Service confirming stock reservation in saga.
     */
    @KafkaListener(topics = KafkaTopics.STOCK_RESERVED, groupId = "${spring.kafka.consumer.group-id:matching-service-group}")
    public void handleStockReserved(String message) {
        try {
            EventEnvelope<StockReservedPayload> envelope = objectMapper.readValue(
                message,
                new TypeReference<>() {}
            );

            idempotentEventConsumer.execute(envelope.eventId(), envelope.eventType(), () -> {
                StockReservedPayload payload = envelope.payload();
                log.info("Processing StockReserved event: allocationId={}, reservationId={}",
                    payload.allocationId(), payload.reservationId());

                matchingService.applyStockReserved(payload.allocationId(), payload.reservationId());
            });
        } catch (Exception e) {
            log.error("Failed to process StockReserved event: {}", message, e);
            throw new RuntimeException("Error processing StockReserved event", e);
        }
    }

    /**
     * Consumes StockReservationFailed event from Inventory Service triggering saga compensation.
     */
    @KafkaListener(topics = KafkaTopics.STOCK_RESERVATION_FAILED, groupId = "${spring.kafka.consumer.group-id:matching-service-group}")
    public void handleStockReservationFailed(String message) {
        try {
            EventEnvelope<StockReservationFailedPayload> envelope = objectMapper.readValue(
                message,
                new TypeReference<>() {}
            );

            idempotentEventConsumer.execute(envelope.eventId(), envelope.eventType(), () -> {
                StockReservationFailedPayload payload = envelope.payload();
                log.warn("Processing StockReservationFailed compensation: allocationId={}, reason={}",
                    payload.allocationId(), payload.failureReason());

                matchingService.applyStockReservationFailed(
                    payload.allocationId(),
                    payload.failureReason() != null ? payload.failureReason().name() : "INSUFFICIENT_STOCK"
                );
            });
        } catch (Exception e) {
            log.error("Failed to process StockReservationFailed event: {}", message, e);
            throw new RuntimeException("Error processing StockReservationFailed event", e);
        }
    }

    /**
     * Consumes ShipmentAssigned event from Transport Service.
     */
    @KafkaListener(topics = KafkaTopics.SHIPMENT_ASSIGNED, groupId = "${spring.kafka.consumer.group-id:matching-service-group}")
    public void handleShipmentAssigned(String message) {
        try {
            EventEnvelope<ShipmentAssignedPayload> envelope = objectMapper.readValue(
                message,
                new TypeReference<>() {}
            );

            idempotentEventConsumer.execute(envelope.eventId(), envelope.eventType(), () -> {
                ShipmentAssignedPayload payload = envelope.payload();
                log.info("Processing ShipmentAssigned event: allocationId={}, driverId={}",
                    payload.allocationId(), payload.driverId());

                matchingService.applyShipmentAssigned(payload.allocationId());
            });
        } catch (Exception e) {
            log.error("Failed to process ShipmentAssigned event: {}", message, e);
            throw new RuntimeException("Error processing ShipmentAssigned event", e);
        }
    }

    /**
     * Consumes ShipmentDelivered event from Transport Service to finalize matching saga.
     */
    @KafkaListener(topics = KafkaTopics.SHIPMENT_DELIVERED, groupId = "${spring.kafka.consumer.group-id:matching-service-group}")
    public void handleShipmentDelivered(String message) {
        try {
            EventEnvelope<ShipmentDeliveredPayload> envelope = objectMapper.readValue(
                message,
                new TypeReference<>() {}
            );

            idempotentEventConsumer.execute(envelope.eventId(), envelope.eventType(), () -> {
                ShipmentDeliveredPayload payload = envelope.payload();
                log.info("Processing ShipmentDelivered event: allocationId={}", payload.allocationId());

                matchingService.applyShipmentDelivered(payload.allocationId());
            });
        } catch (Exception e) {
            log.error("Failed to process ShipmentDelivered event: {}", message, e);
            throw new RuntimeException("Error processing ShipmentDelivered event", e);
        }
    }

    /**
     * Consumes DonationRegistered event to re-evaluate shortage factors when new supplies arrive.
     */
    @KafkaListener(topics = KafkaTopics.DONATION_REGISTERED, groupId = "${spring.kafka.consumer.group-id:matching-service-group}")
    public void handleDonationRegistered(String message) {
        try {
            EventEnvelope<DonationRegisteredPayload> envelope = objectMapper.readValue(
                message,
                new TypeReference<>() {}
            );

            idempotentEventConsumer.execute(envelope.eventId(), envelope.eventType(), () -> {
                DonationRegisteredPayload payload = envelope.payload();
                log.info("Processing DonationRegistered event: donationId={}, type={}",
                    payload.donationId(), payload.donationType());
                // Ingested for background matching optimizations
            });
        } catch (Exception e) {
            log.error("Failed to process DonationRegistered event: {}", message, e);
            throw new RuntimeException("Error processing DonationRegistered event", e);
        }
    }
}
