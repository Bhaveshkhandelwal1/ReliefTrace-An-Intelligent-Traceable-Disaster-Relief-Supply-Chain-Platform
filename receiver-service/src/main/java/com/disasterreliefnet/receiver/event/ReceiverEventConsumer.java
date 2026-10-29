package com.disasterreliefnet.receiver.event;

import com.disasterreliefnet.common.dto.EventEnvelope;
import com.disasterreliefnet.common.event.KafkaTopics;
import com.disasterreliefnet.common.event.payload.AllocationApprovedPayload;
import com.disasterreliefnet.common.event.payload.RequestVerifiedPayload;
import com.disasterreliefnet.common.event.payload.ShipmentDeliveredPayload;
import com.disasterreliefnet.common.idempotency.IdempotentEventConsumer;
import com.disasterreliefnet.receiver.service.ReceiverService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Idempotent Kafka consumer listening for verification results, allocation approvals,
 * and shipment deliveries to keep aid requests up-to-date.
 */
@Component
public class ReceiverEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ReceiverEventConsumer.class);

    private final ReceiverService receiverService;
    private final IdempotentEventConsumer idempotentEventConsumer;
    private final ObjectMapper objectMapper;

    public ReceiverEventConsumer(
        ReceiverService receiverService,
        IdempotentEventConsumer idempotentEventConsumer,
        ObjectMapper objectMapper
    ) {
        this.receiverService = receiverService;
        this.idempotentEventConsumer = idempotentEventConsumer;
        this.objectMapper = objectMapper;
    }

    /**
     * Consumes RequestVerified event emitted by Verification Service.
     */
    @KafkaListener(topics = KafkaTopics.REQUEST_VERIFIED, groupId = "${spring.kafka.consumer.group-id:receiver-service-group}")
    public void handleRequestVerified(String message) {
        try {
            EventEnvelope<RequestVerifiedPayload> envelope = objectMapper.readValue(
                message,
                new TypeReference<>() {}
            );

            idempotentEventConsumer.execute(envelope.eventId(), envelope.eventType(), () -> {
                RequestVerifiedPayload payload = envelope.payload();
                log.info("Processing RequestVerified event: requestId={}, outcome={}, caseId={}",
                    payload.requestId(), payload.outcome(), payload.caseId());

                receiverService.applyVerificationOutcome(
                    payload.requestId(),
                    payload.outcome(),
                    payload.reviewNotes()
                );
            });
        } catch (Exception e) {
            log.error("Failed to process RequestVerified event: {}", message, e);
            throw new RuntimeException("Error processing RequestVerified event", e);
        }
    }

    /**
     * Consumes AllocationApproved event emitted by Matching Service.
     */
    @KafkaListener(topics = KafkaTopics.ALLOCATION_APPROVED, groupId = "${spring.kafka.consumer.group-id:receiver-service-group}")
    public void handleAllocationApproved(String message) {
        try {
            EventEnvelope<AllocationApprovedPayload> envelope = objectMapper.readValue(
                message,
                new TypeReference<>() {}
            );

            idempotentEventConsumer.execute(envelope.eventId(), envelope.eventType(), () -> {
                AllocationApprovedPayload payload = envelope.payload();
                log.info("Processing AllocationApproved event: allocationId={}, requestId={}, warehouseId={}",
                    payload.allocationId(), payload.requestId(), payload.warehouseId());

                receiverService.applyAllocationApproved(
                    payload.requestId(),
                    payload.allocationId()
                );
            });
        } catch (Exception e) {
            log.error("Failed to process AllocationApproved event: {}", message, e);
            throw new RuntimeException("Error processing AllocationApproved event", e);
        }
    }

    /**
     * Consumes ShipmentDelivered event emitted by Transport Service to finalize request fulfillment.
     */
    @KafkaListener(topics = KafkaTopics.SHIPMENT_DELIVERED, groupId = "${spring.kafka.consumer.group-id:receiver-service-group}")
    public void handleShipmentDelivered(String message) {
        try {
            EventEnvelope<ShipmentDeliveredPayload> envelope = objectMapper.readValue(
                message,
                new TypeReference<>() {}
            );

            idempotentEventConsumer.execute(envelope.eventId(), envelope.eventType(), () -> {
                ShipmentDeliveredPayload payload = envelope.payload();
                log.info("Processing ShipmentDelivered event: shipmentId={}, allocationId={}",
                    payload.shipmentId(), payload.allocationId());

                receiverService.applyShipmentDelivered(payload.allocationId());
            });
        } catch (Exception e) {
            log.error("Failed to process ShipmentDelivered event: {}", message, e);
            throw new RuntimeException("Error processing ShipmentDelivered event", e);
        }
    }
}
