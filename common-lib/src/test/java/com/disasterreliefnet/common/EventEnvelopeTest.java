package com.disasterreliefnet.common;

import com.disasterreliefnet.common.dto.EventEnvelope;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EventEnvelopeTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void shouldCreateEventEnvelopeWithDefaults() {
        String payload = "payload data";
        UUID aggregateId = UUID.randomUUID();
        EventEnvelope<String> event = EventEnvelope.of(
            "DonationRegistered",
            "DONATION",
            aggregateId,
            null,
            payload
        );

        assertNotNull(event.eventId());
        assertEquals("DonationRegistered", event.eventType());
        assertEquals("DONATION", event.aggregateType());
        assertEquals(aggregateId.toString(), event.aggregateId());
        assertNotNull(event.occurredAt());
        assertEquals(1, event.version());
        assertNotNull(event.correlationId());
        assertEquals(payload, event.payload());
        assertEquals(event.occurredAt(), event.timestamp());
    }

    @Test
    void shouldSerializeAndDeserializeJsonCorrectly() throws Exception {
        UUID eventId = UUID.randomUUID();
        Instant now = Instant.parse("2026-10-29T10:00:00Z");
        EventEnvelope<String> original = new EventEnvelope<>(
            eventId,
            "RequestCreated",
            "AID_REQUEST",
            "req-456",
            now,
            1,
            "corr-789",
            "{\"qty\": 50}"
        );

        String json = objectMapper.writeValueAsString(original);
        assertTrue(json.contains("occurredAt"));
        assertTrue(json.contains("RequestCreated"));

        EventEnvelope<?> deserialized = objectMapper.readValue(json, EventEnvelope.class);
        assertEquals(eventId, deserialized.eventId());
        assertEquals("RequestCreated", deserialized.eventType());
        assertEquals(1, deserialized.version());
    }
}
