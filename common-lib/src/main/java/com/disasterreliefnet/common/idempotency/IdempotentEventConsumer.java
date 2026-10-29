package com.disasterreliefnet.common.idempotency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Helper component that wraps consumer execution within a transaction and checks the processed_events
 * table to prevent duplicate event execution.
 */
@Component
public class IdempotentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(IdempotentEventConsumer.class);

    private final ProcessedEventRepository processedEventRepository;

    public IdempotentEventConsumer(ProcessedEventRepository processedEventRepository) {
        this.processedEventRepository = processedEventRepository;
    }

    /**
     * Executes the given action idempotently. If the event was previously processed,
     * the action is skipped and the method returns false.
     *
     * @param eventId   unique event ID
     * @param eventType type of event
     * @param handler   business processing action
     * @return true if the event was processed, false if it was skipped as duplicate
     */
    @Transactional
    public boolean execute(UUID eventId, String eventType, Runnable handler) {
        if (processedEventRepository.existsById(eventId)) {
            log.info("Duplicate event detected: id={}, type={}. Skipping to guarantee idempotency.", eventId, eventType);
            return false;
        }

        log.debug("Processing new event: id={}, type={}", eventId, eventType);
        handler.run();

        ProcessedEvent processedEvent = ProcessedEvent.builder()
            .eventId(eventId)
            .eventType(eventType)
            .processedAt(Instant.now())
            .build();
        processedEventRepository.save(processedEvent);
        log.debug("Recorded event as processed: id={}", eventId);
        return true;
    }

    /**
     * Executes the given supplier action idempotently, returning an Optional of the result.
     * If the event was previously processed, returns Optional.empty().
     */
    @Transactional
    public <T> Optional<T> executeWithResult(UUID eventId, String eventType, Supplier<T> handler) {
        if (processedEventRepository.existsById(eventId)) {
            log.info("Duplicate event detected: id={}, type={}. Skipping to guarantee idempotency.", eventId, eventType);
            return Optional.empty();
        }

        T result = handler.get();
        ProcessedEvent processedEvent = ProcessedEvent.builder()
            .eventId(eventId)
            .eventType(eventType)
            .processedAt(Instant.now())
            .build();
        processedEventRepository.save(processedEvent);
        return Optional.ofNullable(result);
    }
}
