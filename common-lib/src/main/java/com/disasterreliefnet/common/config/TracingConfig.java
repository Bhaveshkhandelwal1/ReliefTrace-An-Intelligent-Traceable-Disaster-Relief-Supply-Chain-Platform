package com.disasterreliefnet.common.config;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenTelemetry and Micrometer Tracing configuration.
 * Ensures trace IDs and span IDs are automatically synchronized with SLF4J MDC
 * and forwarded to distributed trace collectors (Zipkin / Tempo).
 */
@Configuration
@ConditionalOnClass(Tracer.class)
public class TracingConfig {

    private static final Logger log = LoggerFactory.getLogger(TracingConfig.class);

    @Bean
    public ObservationHandler<Observation.Context> traceMdcObservationHandler(Tracer tracer) {
        return new ObservationHandler<>() {
            @Override
            public void onStart(Observation.Context context) {
                if (tracer.currentSpan() != null) {
                    String traceId = tracer.currentSpan().context().traceId();
                    String spanId = tracer.currentSpan().context().spanId();
                    if (traceId != null) MDC.put("traceId", traceId);
                    if (spanId != null) MDC.put("spanId", spanId);
                }
            }

            @Override
            public void onStop(Observation.Context context) {
                // Keep correlationId; tracing IDs are managed by the tracer lifecycle
            }

            @Override
            public boolean supportsContext(Observation.Context context) {
                return true;
            }
        };
    }
}
