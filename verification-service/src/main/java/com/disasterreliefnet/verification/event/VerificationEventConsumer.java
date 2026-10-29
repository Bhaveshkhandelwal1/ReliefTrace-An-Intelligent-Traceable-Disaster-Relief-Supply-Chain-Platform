package com.disasterreliefnet.verification.event;

import com.disasterreliefnet.common.dto.EventEnvelope;
import com.disasterreliefnet.common.event.KafkaTopics;
import com.disasterreliefnet.common.event.payload.DisasterDeclaredPayload;
import com.disasterreliefnet.common.event.payload.RequestCreatedPayload;
import com.disasterreliefnet.common.idempotency.IdempotentEventConsumer;
import com.disasterreliefnet.verification.service.VerificationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Idempotent Kafka consumer listening for incoming aid requests and declared disasters.
 */
@Component
public class VerificationEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(VerificationEventConsumer.class);

    private final VerificationService verificationService;
    private final IdempotentEventConsumer idempotentEventConsumer;
    private final ObjectMapper objectMapper;

    public VerificationEventConsumer(
        VerificationService verificationService,
        IdempotentEventConsumer idempotentEventConsumer,
        ObjectMapper objectMapper
    ) {
        this.verificationService = verificationService;
        this.idempotentEventConsumer = idempotentEventConsumer;
        this.objectMapper = objectMapper;
    }

    /**
     * Consumes RequestCreated event from Receiver Service and opens a verification case.
     */
    @KafkaListener(topics = KafkaTopics.REQUEST_CREATED, groupId = "${spring.kafka.consumer.group-id:verification-service-group}")
    public void handleRequestCreated(String message) {
        try {
            EventEnvelope<RequestCreatedPayload> envelope = objectMapper.readValue(
                message,
                new TypeReference<>() {}
            );

            idempotentEventConsumer.execute(envelope.eventId(), envelope.eventType(), () -> {
                RequestCreatedPayload payload = envelope.payload();
                log.info("Processing RequestCreated event: requestId={}, receiverId={}, category={}",
                    payload.requestId(), payload.receiverId(), payload.category());

                verificationService.openCase(payload.requestId(), payload.receiverId());
            });
        } catch (Exception e) {
            log.error("Failed to process RequestCreated event: {}", message, e);
            throw new RuntimeException("Error processing RequestCreated event", e);
        }
    }

    /**
     * Consumes DisasterDeclared event to track disaster boundaries for geographic verification.
     */
    @KafkaListener(topics = KafkaTopics.DISASTER_DECLARED, groupId = "${spring.kafka.consumer.group-id:verification-service-group}")
    public void handleDisasterDeclared(String message) {
        try {
            EventEnvelope<DisasterDeclaredPayload> envelope = objectMapper.readValue(
                message,
                new TypeReference<>() {}
            );

            idempotentEventConsumer.execute(envelope.eventId(), envelope.eventType(), () -> {
                DisasterDeclaredPayload payload = envelope.payload();
                log.info("Processing DisasterDeclared event: disasterId={}, name={}, severity={}",
                    payload.disasterId(), payload.name(), payload.severityLevel());
                // Stored/logged for reference in geo-validation
            });
        } catch (Exception e) {
            log.error("Failed to process DisasterDeclared event: {}", message, e);
            throw new RuntimeException("Error processing DisasterDeclared event", e);
        }
    }
}
