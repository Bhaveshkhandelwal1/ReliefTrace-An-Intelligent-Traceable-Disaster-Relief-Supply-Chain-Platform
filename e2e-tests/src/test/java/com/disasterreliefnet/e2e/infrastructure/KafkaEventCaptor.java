package com.disasterreliefnet.e2e.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Utility that creates a short-lived Kafka consumer to verify that an event with
 * a specific {@code eventType} field was published to a topic within a timeout.
 *
 * <p>Used in assertions like:
 * <pre>
 *   KafkaEventCaptor.assertEventPublished(KAFKA, "stock.reserved", "StockReserved", 30);
 * </pre>
 */
public final class KafkaEventCaptor {

    private static final ObjectMapper MAPPER = new ObjectMapper()
        .registerModule(new JavaTimeModule());

    private KafkaEventCaptor() {}

    /**
     * Asserts that at least one message with {@code eventType} appears on the
     * given topic within {@code timeoutSeconds}.
     *
     * @param kafka          running Kafka container
     * @param topic          Kafka topic to consume
     * @param eventType      expected value of the {@code "eventType"} JSON field
     * @param timeoutSeconds how many seconds to wait at most
     * @throws AssertionError if no matching event arrives in time
     */
    public static void assertEventPublished(
        org.testcontainers.containers.KafkaContainer kafka,
        String topic,
        String eventType,
        int timeoutSeconds
    ) {
        List<Map<String, Object>> found = poll(kafka, topic, timeoutSeconds);
        boolean matched = found.stream().anyMatch(msg -> eventType.equals(msg.get("eventType")));
        if (!matched) {
            throw new AssertionError("No event of type [" + eventType + "] arrived on topic ["
                + topic + "] within " + timeoutSeconds + "s. Events received: " + found.size());
        }
    }

    /**
     * Returns all messages received from {@code topic} within {@code timeoutSeconds},
     * each deserialized as a {@code Map<String, Object>} representing the event envelope.
     */
    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> poll(
        org.testcontainers.containers.KafkaContainer kafka,
        String topic,
        int timeoutSeconds
    ) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "e2e-captor-" + System.nanoTime());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "100");

        List<Map<String, Object>> results = new ArrayList<>();
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(timeoutSeconds);

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(topic));
            while (System.currentTimeMillis() < deadline) {
                ConsumerRecords<String, String> records =
                    consumer.poll(Duration.ofMillis(500));
                records.forEach(r -> {
                    try {
                        results.add(MAPPER.readValue(r.value(), Map.class));
                    } catch (JsonProcessingException ignored) { /* skip malformed */ }
                });
                if (!results.isEmpty() && System.currentTimeMillis() > deadline - 2000) {
                    break;
                }
            }
        }
        return results;
    }

    /**
     * Counts messages on {@code topic} with the given {@code eventType} that arrive
     * within {@code timeoutSeconds}.
     */
    public static long countEvents(
        org.testcontainers.containers.KafkaContainer kafka,
        String topic,
        String eventType,
        int timeoutSeconds
    ) {
        return poll(kafka, topic, timeoutSeconds).stream()
            .filter(msg -> eventType.equals(msg.get("eventType")))
            .count();
    }
}
