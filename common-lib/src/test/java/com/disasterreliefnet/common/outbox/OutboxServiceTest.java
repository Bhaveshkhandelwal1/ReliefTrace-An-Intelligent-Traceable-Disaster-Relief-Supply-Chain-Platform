package com.disasterreliefnet.common.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxServiceTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    private OutboxService outboxService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        outboxService = new OutboxService(outboxEventRepository, objectMapper);
    }

    @Test
    void shouldEnqueueOutboxEventCorrectly() {
        UUID aggregateId = UUID.randomUUID();
        Map<String, String> payload = Map.of("key", "value");

        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(i -> i.getArgument(0));

        OutboxEvent result = outboxService.enqueue(
            "test.topic",
            "TEST_AGGREGATE",
            aggregateId,
            "TestCreated",
            payload,
            "corr-123"
        );

        assertNotNull(result);
        assertEquals("test.topic", result.getTopic());
        assertEquals("TEST_AGGREGATE", result.getAggregateType());
        assertEquals(aggregateId, result.getAggregateId());
        assertEquals("TestCreated", result.getEventType());
        assertFalse(result.isPublished());
        assertNotNull(result.getPayload());
        assertTrue(result.getPayload().contains("corr-123"));

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository, times(1)).save(captor.capture());
        assertEquals("test.topic", captor.getValue().getTopic());
    }
}
