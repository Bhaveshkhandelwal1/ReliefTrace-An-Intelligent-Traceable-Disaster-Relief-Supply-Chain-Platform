package com.disasterreliefnet.common.config;

import com.disasterreliefnet.common.exception.GlobalExceptionHandler;
import com.disasterreliefnet.common.idempotency.IdempotentEventConsumer;
import com.disasterreliefnet.common.outbox.OutboxPublisher;
import com.disasterreliefnet.common.outbox.OutboxService;
import com.disasterreliefnet.common.security.KeycloakJwtAuthenticationConverter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Auto-configuration for DisasterReliefNet common library components.
 * Automatically activates Outbox, Idempotency, Global Exception Handling, Security helpers, and Kafka configuration.
 */
@AutoConfiguration
@EnableScheduling
@EntityScan(basePackages = "com.disasterreliefnet")
@EnableJpaRepositories(basePackages = "com.disasterreliefnet")
@Import({
    GlobalExceptionHandler.class,
    OutboxService.class,
    OutboxPublisher.class,
    IdempotentEventConsumer.class,
    CommonKafkaConfig.class
})
public class DisasterReliefCommonAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public KeycloakJwtAuthenticationConverter keycloakJwtAuthenticationConverter() {
        return new KeycloakJwtAuthenticationConverter();
    }
}
