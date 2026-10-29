package com.disasterreliefnet.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.Callable;

/**
 * Central business metrics registry helper for DisasterReliefNet platform.
 * Exposes domain counters, timers, and gauges scraped by Prometheus.
 */
@Component
public class DisasterReliefMetrics {

    private final MeterRegistry registry;

    private final Counter donationsRegisteredCounter;
    private final Counter aidRequestsCreatedCounter;
    private final Counter allocationsApprovedCounter;
    private final Counter shipmentsDeliveredCounter;
    private final Counter kafkaEventsPublishedCounter;
    private final Counter kafkaEventsConsumedCounter;
    private final Counter circuitBreakerTripsCounter;
    private final Timer sagaDurationTimer;

    public DisasterReliefMetrics(MeterRegistry registry) {
        this.registry = registry;

        this.donationsRegisteredCounter = Counter.builder("disasterrelief.donations.registered.total")
            .description("Total number of donations registered in the platform")
            .register(registry);

        this.aidRequestsCreatedCounter = Counter.builder("disasterrelief.aid_requests.created.total")
            .description("Total number of relief aid requests submitted by affected camps")
            .register(registry);

        this.allocationsApprovedCounter = Counter.builder("disasterrelief.allocations.approved.total")
            .description("Total number of aid allocations approved by coordinators")
            .register(registry);

        this.shipmentsDeliveredCounter = Counter.builder("disasterrelief.shipments.delivered.total")
            .description("Total number of relief shipments confirmed delivered")
            .register(registry);

        this.kafkaEventsPublishedCounter = Counter.builder("disasterrelief.kafka.events.published.total")
            .description("Total number of domain event envelopes published via Transactional Outbox")
            .register(registry);

        this.kafkaEventsConsumedCounter = Counter.builder("disasterrelief.kafka.events.consumed.total")
            .description("Total number of domain event envelopes consumed idempotently")
            .register(registry);

        this.circuitBreakerTripsCounter = Counter.builder("disasterrelief.resilience.circuit_breaker.trips.total")
            .description("Total number of times a Resilience4j circuit breaker entered OPEN state")
            .register(registry);

        this.sagaDurationTimer = Timer.builder("disasterrelief.saga.execution.duration")
            .description("End-to-end execution duration of distributed saga workflows")
            .publishPercentiles(0.5, 0.9, 0.95, 0.99)
            .register(registry);
    }

    public void incrementDonationsRegistered() {
        donationsRegisteredCounter.increment();
    }

    public void incrementAidRequestsCreated() {
        aidRequestsCreatedCounter.increment();
    }

    public void incrementAllocationsApproved() {
        allocationsApprovedCounter.increment();
    }

    public void incrementShipmentsDelivered() {
        shipmentsDeliveredCounter.increment();
    }

    public void incrementKafkaPublished(String eventType) {
        registry.counter("disasterrelief.kafka.events.published", "eventType", eventType).increment();
        kafkaEventsPublishedCounter.increment();
    }

    public void incrementKafkaConsumed(String eventType) {
        registry.counter("disasterrelief.kafka.events.consumed", "eventType", eventType).increment();
        kafkaEventsConsumedCounter.increment();
    }

    public void incrementCircuitBreakerTrips(String circuitBreakerName) {
        registry.counter("disasterrelief.resilience.circuit_breaker.trips", "name", circuitBreakerName).increment();
        circuitBreakerTripsCounter.increment();
    }

    public void recordSagaDuration(Duration duration) {
        sagaDurationTimer.record(duration);
    }

    public <T> T recordSagaExecution(Callable<T> callable) throws Exception {
        return sagaDurationTimer.recordCallable(callable);
    }

    public MeterRegistry getRegistry() {
        return registry;
    }
}
