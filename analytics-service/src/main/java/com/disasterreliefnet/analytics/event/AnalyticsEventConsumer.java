package com.disasterreliefnet.analytics.event;

import com.disasterreliefnet.analytics.service.AnalyticsFactService;
import com.disasterreliefnet.common.dto.EventEnvelope;
import com.disasterreliefnet.common.event.KafkaTopics;
import com.disasterreliefnet.common.event.payload.*;
import com.disasterreliefnet.common.idempotency.IdempotentEventConsumer;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Event-driven consumer materialising analytics fact tables from platform events.
 */
@Component
public class AnalyticsEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsEventConsumer.class);

    private final AnalyticsFactService factService;
    private final IdempotentEventConsumer idempotentEventConsumer;
    private final ObjectMapper objectMapper;

    public AnalyticsEventConsumer(
        AnalyticsFactService factService,
        IdempotentEventConsumer idempotentEventConsumer,
        ObjectMapper objectMapper
    ) {
        this.factService = factService;
        this.idempotentEventConsumer = idempotentEventConsumer;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = KafkaTopics.DONATION_REGISTERED, groupId = "${spring.kafka.consumer.group-id:analytics-service-group}")
    public void handleDonationRegistered(String message) {
        try {
            EventEnvelope<DonationRegisteredPayload> envelope = objectMapper.readValue(message, new TypeReference<>() {});
            idempotentEventConsumer.execute(envelope.eventId(), envelope.eventType(), () -> {
                log.info("Materialising donation fact from event: {}", envelope.eventId());
                factService.recordDonation(envelope.payload());
            });
        } catch (Exception ex) {
            log.error("Failed to process DonationRegistered event: {}", ex.getMessage(), ex);
        }
    }

    @KafkaListener(topics = KafkaTopics.REQUEST_CREATED, groupId = "${spring.kafka.consumer.group-id:analytics-service-group}")
    public void handleRequestCreated(String message) {
        try {
            EventEnvelope<RequestCreatedPayload> envelope = objectMapper.readValue(message, new TypeReference<>() {});
            idempotentEventConsumer.execute(envelope.eventId(), envelope.eventType(), () -> {
                log.info("Materialising request fact from event: {}", envelope.eventId());
                factService.recordRequestCreated(envelope.payload());
            });
        } catch (Exception ex) {
            log.error("Failed to process RequestCreated event: {}", ex.getMessage(), ex);
        }
    }

    @KafkaListener(topics = KafkaTopics.REQUEST_VERIFIED, groupId = "${spring.kafka.consumer.group-id:analytics-service-group}")
    public void handleRequestVerified(String message) {
        try {
            EventEnvelope<RequestVerifiedPayload> envelope = objectMapper.readValue(message, new TypeReference<>() {});
            idempotentEventConsumer.execute(envelope.eventId(), envelope.eventType(), () -> {
                log.info("Updating request fact from RequestVerified event: {}", envelope.eventId());
                factService.recordRequestVerified(envelope.payload());
            });
        } catch (Exception ex) {
            log.error("Failed to process RequestVerified event: {}", ex.getMessage(), ex);
        }
    }

    @KafkaListener(topics = KafkaTopics.ALLOCATION_APPROVED, groupId = "${spring.kafka.consumer.group-id:analytics-service-group}")
    public void handleAllocationApproved(String message) {
        try {
            EventEnvelope<AllocationApprovedPayload> envelope = objectMapper.readValue(message, new TypeReference<>() {});
            idempotentEventConsumer.execute(envelope.eventId(), envelope.eventType(), () -> {
                log.info("Updating request fact from AllocationApproved event: {}", envelope.eventId());
                factService.recordAllocationApproved(envelope.payload());
            });
        } catch (Exception ex) {
            log.error("Failed to process AllocationApproved event: {}", ex.getMessage(), ex);
        }
    }

    @KafkaListener(topics = KafkaTopics.SHIPMENT_ASSIGNED, groupId = "${spring.kafka.consumer.group-id:analytics-service-group}")
    public void handleShipmentAssigned(String message) {
        try {
            EventEnvelope<ShipmentAssignedPayload> envelope = objectMapper.readValue(message, new TypeReference<>() {});
            idempotentEventConsumer.execute(envelope.eventId(), envelope.eventType(), () -> {
                log.info("Materialising shipment fact from ShipmentAssigned event: {}", envelope.eventId());
                factService.recordShipmentAssigned(envelope.payload());
            });
        } catch (Exception ex) {
            log.error("Failed to process ShipmentAssigned event: {}", ex.getMessage(), ex);
        }
    }

    @KafkaListener(topics = KafkaTopics.SHIPMENT_DELIVERED, groupId = "${spring.kafka.consumer.group-id:analytics-service-group}")
    public void handleShipmentDelivered(String message) {
        try {
            EventEnvelope<ShipmentDeliveredPayload> envelope = objectMapper.readValue(message, new TypeReference<>() {});
            idempotentEventConsumer.execute(envelope.eventId(), envelope.eventType(), () -> {
                log.info("Updating shipment fact from ShipmentDelivered event: {}", envelope.eventId());
                factService.recordShipmentDelivered(envelope.payload());
            });
        } catch (Exception ex) {
            log.error("Failed to process ShipmentDelivered event: {}", ex.getMessage(), ex);
        }
    }

    @KafkaListener(topics = KafkaTopics.DISASTER_DECLARED, groupId = "${spring.kafka.consumer.group-id:analytics-service-group}")
    public void handleDisasterDeclared(String message) {
        try {
            EventEnvelope<DisasterDeclaredPayload> envelope = objectMapper.readValue(message, new TypeReference<>() {});
            idempotentEventConsumer.execute(envelope.eventId(), envelope.eventType(), () -> {
                log.info("Materialising disaster summary from DisasterDeclared event: {}", envelope.eventId());
                factService.recordDisasterDeclared(envelope.payload());
            });
        } catch (Exception ex) {
            log.error("Failed to process DisasterDeclared event: {}", ex.getMessage(), ex);
        }
    }

    @KafkaListener(topics = KafkaTopics.DISASTER_CLOSED, groupId = "${spring.kafka.consumer.group-id:analytics-service-group}")
    public void handleDisasterClosed(String message) {
        try {
            EventEnvelope<DisasterClosedPayload> envelope = objectMapper.readValue(message, new TypeReference<>() {});
            idempotentEventConsumer.execute(envelope.eventId(), envelope.eventType(), () -> {
                log.info("Closing disaster summary from DisasterClosed event: {}", envelope.eventId());
                factService.recordDisasterClosed(envelope.payload());
            });
        } catch (Exception ex) {
            log.error("Failed to process DisasterClosed event: {}", ex.getMessage(), ex);
        }
    }
}
