package com.disasterreliefnet.common.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Scheduled background worker that polls unpublished outbox events using SKIP LOCKED,
 * publishes them to Kafka, and marks them as published.
 */
@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final int batchSize;

    public OutboxPublisher(
        OutboxEventRepository outboxEventRepository,
        KafkaTemplate<String, String> kafkaTemplate,
        @Value("${outbox.publisher.batch-size:50}") int batchSize
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.batchSize = batchSize;
    }

    @Scheduled(
        fixedDelayString = "${outbox.publisher.fixed-delay-ms:1000}",
        initialDelayString = "${outbox.publisher.initial-delay-ms:2000}"
    )
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> events;
        try {
            events = outboxEventRepository.findUnpublishedForUpdateSkipLocked(batchSize);
        } catch (Exception ex) {
            log.warn("Database does not support FOR UPDATE SKIP LOCKED or query failed; falling back to top batch: {}", ex.getMessage());
            events = outboxEventRepository.findTop100ByPublishedFalseOrderByCreatedAtAsc();
        }

        if (events == null || events.isEmpty()) {
            return;
        }

        log.debug("Found {} pending outbox events to publish", events.size());
        for (OutboxEvent event : events) {
            publishSingleEvent(event);
        }
    }

    private void publishSingleEvent(OutboxEvent event) {
        String key = event.getAggregateId() != null ? event.getAggregateId().toString() : event.getEventId().toString();
        try {
            log.info("Publishing outbox event {} to topic {} with key {}", event.getEventId(), event.getTopic(), key);
            kafkaTemplate.send(event.getTopic(), key, event.getPayload()).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish outbox event {} to topic {}", event.getEventId(), event.getTopic(), ex);
                } else {
                    log.debug("Successfully published outbox event {} at offset {}", event.getEventId(),
                        result != null && result.getRecordMetadata() != null ? result.getRecordMetadata().offset() : -1);
                }
            });

            event.markPublished();
            outboxEventRepository.save(event);
        } catch (Exception ex) {
            log.error("Synchronous error sending outbox event {} to Kafka: {}", event.getEventId(), ex.getMessage(), ex);
        }
    }
}
