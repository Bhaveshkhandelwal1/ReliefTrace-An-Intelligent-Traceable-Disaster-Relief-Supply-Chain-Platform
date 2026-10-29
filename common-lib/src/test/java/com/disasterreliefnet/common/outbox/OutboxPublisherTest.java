package com.disasterreliefnet.common.outbox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private OutboxPublisher outboxPublisher;

    @BeforeEach
    void setUp() {
        outboxPublisher = new OutboxPublisher(outboxEventRepository, kafkaTemplate, 50);
    }

    @Test
    void shouldPublishPendingEventsAndMarkThemAsPublished() {
        UUID eventId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();
        OutboxEvent event = OutboxEvent.builder()
            .eventId(eventId)
            .aggregateType("DONATION")
            .aggregateId(aggregateId)
            .eventType("DonationRegistered")
            .topic("donation.registered")
            .payload("{\"donationId\":\"" + aggregateId + "\"}")
            .published(false)
            .createdAt(Instant.now())
            .build();

        when(outboxEventRepository.findUnpublishedForUpdateSkipLocked(50)).thenReturn(List.of(event));
        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(mock(SendResult.class));
        when(kafkaTemplate.send(eq("donation.registered"), eq(aggregateId.toString()), eq(event.getPayload())))
            .thenReturn(future);

        outboxPublisher.publishPendingEvents();

        verify(kafkaTemplate, times(1)).send(eq("donation.registered"), eq(aggregateId.toString()), eq(event.getPayload()));
        assertTrue(event.isPublished());
        verify(outboxEventRepository, times(1)).save(event);
    }

    @Test
    void shouldFallbackWhenSkipLockedThrowsException() {
        UUID eventId = UUID.randomUUID();
        OutboxEvent event = OutboxEvent.builder()
            .eventId(eventId)
            .aggregateType("DONATION")
            .aggregateId(UUID.randomUUID())
            .eventType("DonationRegistered")
            .topic("donation.registered")
            .payload("{}")
            .published(false)
            .createdAt(Instant.now())
            .build();

        when(outboxEventRepository.findUnpublishedForUpdateSkipLocked(50)).thenThrow(new RuntimeException("SKIP LOCKED unsupported"));
        when(outboxEventRepository.findTop100ByPublishedFalseOrderByCreatedAtAsc()).thenReturn(List.of(event));
        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(mock(SendResult.class));
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(future);

        outboxPublisher.publishPendingEvents();

        assertTrue(event.isPublished());
        verify(outboxEventRepository, times(1)).save(event);
    }
}
