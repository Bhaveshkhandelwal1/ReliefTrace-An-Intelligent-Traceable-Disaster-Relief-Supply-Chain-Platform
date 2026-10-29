package com.disasterreliefnet.common.idempotency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotentEventConsumerTest {

    @Mock
    private ProcessedEventRepository processedEventRepository;

    private IdempotentEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new IdempotentEventConsumer(processedEventRepository);
    }

    @Test
    void shouldExecuteActionWhenEventHasNotBeenProcessed() {
        UUID eventId = UUID.randomUUID();
        when(processedEventRepository.existsById(eventId)).thenReturn(false);

        AtomicBoolean executed = new AtomicBoolean(false);
        boolean result = consumer.execute(eventId, "TestEvent", () -> executed.set(true));

        assertTrue(result);
        assertTrue(executed.get());
        verify(processedEventRepository, times(1)).save(any(ProcessedEvent.class));
    }

    @Test
    void shouldSkipActionWhenEventHasAlreadyBeenProcessed() {
        UUID eventId = UUID.randomUUID();
        when(processedEventRepository.existsById(eventId)).thenReturn(true);

        AtomicBoolean executed = new AtomicBoolean(false);
        boolean result = consumer.execute(eventId, "TestEvent", () -> executed.set(true));

        assertFalse(result);
        assertFalse(executed.get());
        verify(processedEventRepository, never()).save(any(ProcessedEvent.class));
    }
}
