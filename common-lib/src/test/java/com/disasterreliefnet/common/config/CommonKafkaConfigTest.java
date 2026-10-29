package com.disasterreliefnet.common.config;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class CommonKafkaConfigTest {

    @Test
    void shouldResolveDeadLetterTopicCorrectly() {
        CommonKafkaConfig config = new CommonKafkaConfig();
        ConsumerRecord<Object, Object> record = new ConsumerRecord<>("donation.registered", 1, 10L, "key", "val");
        Exception ex = new RuntimeException("Kafka error");

        TopicPartition destination = config.resolveDeadLetterTopic(record, ex);
        assertEquals("donation.registered.dlt", destination.topic());
        assertEquals(1, destination.partition());
    }

    @Test
    void shouldCreateRecovererAndErrorHandlerBeans() {
        @SuppressWarnings("unchecked")
        KafkaOperations<Object, Object> kafkaOperations = mock(KafkaOperations.class);
        CommonKafkaConfig config = new CommonKafkaConfig();

        DeadLetterPublishingRecoverer recoverer = config.deadLetterPublishingRecoverer(kafkaOperations);
        assertNotNull(recoverer);

        DefaultErrorHandler errorHandler = config.defaultKafkaErrorHandler(recoverer);
        assertNotNull(errorHandler);
    }
}
