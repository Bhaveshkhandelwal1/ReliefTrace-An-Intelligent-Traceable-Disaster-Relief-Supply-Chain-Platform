package com.disasterreliefnet.common.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

/**
 * Standard transactional event envelope used across all Kafka topics in DisasterReliefNet.
 * Adheres to the event catalogue schema in ARCHITECTURE.md Section 4.
 *
 * @param eventId       unique identifier for the event
 * @param eventType     name of the domain event (e.g. DonationRegistered, RequestCreated)
 * @param aggregateType aggregate root type (e.g. DONOR, DONATION, AID_REQUEST)
 * @param aggregateId   ID of the aggregate
 * @param occurredAt    event occurrence timestamp (UTC)
 * @param version       schema version (defaults to 1)
 * @param correlationId correlation ID propagated across service boundaries
 * @param payload       event domain payload
 * @param <T>           type of the payload
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EventEnvelope<T>(
    @NotNull UUID eventId,
    @NotBlank String eventType,
    String aggregateType,
    String aggregateId,
    @NotNull @JsonAlias("timestamp") Instant occurredAt,
    int version,
    String correlationId,
    @NotNull T payload
) {
    public EventEnvelope {
        if (version <= 0) {
            version = 1;
        }
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
    }

    public static <T> EventEnvelope<T> of(String eventType, String aggregateType, String aggregateId, String correlationId, T payload) {
        return new EventEnvelope<>(
            UUID.randomUUID(),
            eventType,
            aggregateType,
            aggregateId,
            Instant.now(),
            1,
            correlationId != null ? correlationId : UUID.randomUUID().toString(),
            payload
        );
    }

    public static <T> EventEnvelope<T> of(String eventType, String aggregateType, UUID aggregateId, String correlationId, T payload) {
        return of(eventType, aggregateType, aggregateId != null ? aggregateId.toString() : null, correlationId, payload);
    }

    public static <T> EventEnvelope<T> of(String eventType, T payload) {
        return of(eventType, null, (String) null, null, payload);
    }

    /**
     * Backward compatibility accessor for timestamp.
     */
    public Instant timestamp() {
        return occurredAt;
    }
}
