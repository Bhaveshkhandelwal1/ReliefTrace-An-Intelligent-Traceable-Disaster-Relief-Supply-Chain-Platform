package com.disasterreliefnet.gateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void shouldGenerateNewCorrelationIdWhenHeaderIsMissing() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/donors").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicReference<String> downstreamCorrelationId = new AtomicReference<>();
        GatewayFilterChain chain = mutatedExchange -> {
            downstreamCorrelationId.set(mutatedExchange.getRequest().getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER));
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertNotNull(downstreamCorrelationId.get());
        assertFalse(downstreamCorrelationId.get().isBlank());
    }

    @Test
    void shouldPreserveExistingCorrelationId() {
        String existingId = "client-correlation-id-999";
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/donors")
            .header(CorrelationIdFilter.CORRELATION_ID_HEADER, existingId)
            .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicReference<String> downstreamCorrelationId = new AtomicReference<>();
        GatewayFilterChain chain = mutatedExchange -> {
            downstreamCorrelationId.set(mutatedExchange.getRequest().getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER));
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertEquals(existingId, downstreamCorrelationId.get());
    }
}
