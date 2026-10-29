package com.disasterreliefnet.e2e.infrastructure;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.testcontainers.containers.KafkaContainer;

import java.time.Instant;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * Utility to directly publish test events to Kafka topics, bypassing the
 * transactional outbox. Used in failure-scenario tests (e.g., duplicate event,
 * notification service down).
 */
public final class KafkaEventPublisher {

    private KafkaEventPublisher() {}

    /**
     * Publishes a raw JSON string to the given Kafka topic.
     *
     * @param kafka   running Kafka container
     * @param topic   target topic
     * @param key     message key (usually the aggregate ID)
     * @param payload raw JSON string (the event envelope)
     * @throws RuntimeException if publishing fails
     */
    public static void publish(KafkaContainer kafka, String topic, String key, String payload) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, payload);
            producer.send(record).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while publishing to Kafka", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Failed to publish to Kafka topic " + topic, e);
        }
    }

    /**
     * Builds a standard DisasterReliefNet event envelope JSON string.
     *
     * @param eventType   e.g. {@code "StockReserved"}
     * @param payloadJson inner payload JSON object string
     * @return full envelope JSON
     */
    public static String buildEnvelope(String eventType, String aggregateType,
                                       String aggregateId, String payloadJson) {
        return buildEnvelope(UUID.randomUUID(), eventType, aggregateType, aggregateId, payloadJson);
    }

    /**
     * Builds a standard DisasterReliefNet event envelope JSON string with an explicit eventId.
     */
    public static String buildEnvelope(UUID eventId, String eventType, String aggregateType,
                                       String aggregateId, String payloadJson) {
        return """
            {
              "eventId":   "%s",
              "eventType": "%s",
              "aggregateType": "%s",
              "aggregateId": "%s",
              "occurredAt": "%s",
              "version":   1,
              "correlationId": "e2e-test",
              "payload":   %s
            }
            """.formatted(eventId, eventType, aggregateType, aggregateId,
                Instant.now().toString(), payloadJson);
    }
}
