package com.disasterreliefnet.common.outbox;

import com.disasterreliefnet.common.dto.EventEnvelope;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Service facilitating transactional enqueue of domain events into the outbox table.
 */
@Service
public class OutboxService {

    private static final Logger log = LoggerFactory.getLogger(OutboxService.class);

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Enqueues an event within the caller's active database transaction.
     *
     * @param topic         target Kafka topic
     * @param aggregateType aggregate root name (e.g. DONOR, AID_REQUEST)
     * @param aggregateId   aggregate root ID
     * @param eventType     event type identifier
     * @param payload       event payload record
     * @param correlationId correlation tracking ID
     * @param <T>           payload type
     * @return persisted OutboxEvent
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public <T> OutboxEvent enqueue(String topic, String aggregateType, UUID aggregateId, String eventType, T payload, String correlationId) {
        UUID eventId = UUID.randomUUID();
        EventEnvelope<T> envelope = new EventEnvelope<>(
            eventId,
            eventType,
            aggregateType,
            aggregateId != null ? aggregateId.toString() : null,
            Instant.now(),
            1,
            correlationId,
            payload
        );

        try {
            String payloadJson = objectMapper.writeValueAsString(envelope);
            OutboxEvent outboxEvent = OutboxEvent.builder()
                .eventId(eventId)
                .aggregateType(aggregateType)
                .aggregateId(aggregateId != null ? aggregateId : UUID.randomUUID())
                .eventType(eventType)
                .topic(topic)
                .payload(payloadJson)
                .published(false)
                .createdAt(Instant.now())
                .build();

            log.debug("Enqueuing outbox event: id={}, type={}, topic={}", eventId, eventType, topic);
            return outboxEventRepository.save(outboxEvent);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize outbox event payload for event type {}", eventType, e);
            throw new IllegalArgumentException("Could not serialize event payload to JSON", e);
        }
    }
}
