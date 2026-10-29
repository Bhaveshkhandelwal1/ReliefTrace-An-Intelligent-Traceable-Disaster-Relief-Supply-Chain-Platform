package com.disasterreliefnet.common.config;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

/**
 * Common Kafka configuration providing standard error handling with exponential backoff
 * and dead-letter topic (DLT) forwarding on exhausted retries.
 */
@Configuration
@ConditionalOnClass(KafkaTemplate.class)
public class CommonKafkaConfig {

    private static final Logger log = LoggerFactory.getLogger(CommonKafkaConfig.class);

    @Value("${kafka.retry.initial-interval-ms:2000}")
    private long initialIntervalMs = 2000L;

    @Value("${kafka.retry.multiplier:2.0}")
    private double multiplier = 2.0;

    @Value("${kafka.retry.max-interval-ms:8000}")
    private long maxIntervalMs = 8000L;

    @Value("${kafka.retry.max-attempts:3}")
    private int maxAttempts = 3;

    /**
     * Resolves the target TopicPartition for dead-letter publishing, appending '.dlt' to original topic.
     */
    public TopicPartition resolveDeadLetterTopic(ConsumerRecord<?, ?> record, Exception exception) {
        String dltTopic = record.topic() + ".dlt";
        log.error("Exhausted retries for topic={} partition={} offset={}. Forwarding message to DLT={}. Error: {}",
            record.topic(), record.partition(), record.offset(), dltTopic, exception.getMessage());
        return new TopicPartition(dltTopic, record.partition());
    }

    /**
     * Dead letter publishing recoverer that routes exhausted failed messages to '{original-topic}.dlt'.
     */
    @Bean
    @ConditionalOnMissingBean
    public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(KafkaOperations<Object, Object> kafkaOperations) {
        return new DeadLetterPublishingRecoverer(kafkaOperations, this::resolveDeadLetterTopic);
    }

    /**
     * Configures the DefaultErrorHandler with exponential backoff and DLT recovery.
     */
    @Bean
    @ConditionalOnMissingBean(CommonErrorHandler.class)
    public DefaultErrorHandler defaultKafkaErrorHandler(DeadLetterPublishingRecoverer recoverer) {
        ExponentialBackOff backOff = new ExponentialBackOff(initialIntervalMs, multiplier);
        backOff.setMaxInterval(maxIntervalMs);
        backOff.setMaxElapsedTime(maxIntervalMs * maxAttempts);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);
        errorHandler.setRetryListeners((record, ex, deliveryAttempt) ->
            log.warn("Kafka retry attempt {} for topic {} partition {} offset {} due to: {}",
                deliveryAttempt, record.topic(), record.partition(), record.offset(), ex.getMessage())
        );

        return errorHandler;
    }

    /**
     * Customizes ConcurrentKafkaListenerContainerFactory with the common error handler.
     */
    @Bean
    @ConditionalOnMissingBean(name = "kafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<Object, Object> kafkaListenerContainerFactory(
        ConsumerFactory<Object, Object> consumerFactory,
        CommonErrorHandler commonErrorHandler
    ) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(commonErrorHandler);
        return factory;
    }
}
