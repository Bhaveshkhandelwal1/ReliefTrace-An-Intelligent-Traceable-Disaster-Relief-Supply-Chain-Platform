package com.disasterreliefnet.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.function.Function;

/**
 * Filter that propagates or generates an X-Correlation-Id header across all gateway requests.
 * Attaches the correlation ID to the downstream request and returns it in the HTTP response.
 */
@Component
public class CorrelationIdFilter implements GlobalFilter, WebFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return process(exchange, chain::filter);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return process(exchange, chain::filter);
    }

    private Mono<Void> process(ServerWebExchange exchange, Function<ServerWebExchange, Mono<Void>> next) {
        ServerHttpRequest request = exchange.getRequest();
        String correlationId = request.getHeaders().getFirst(CORRELATION_ID_HEADER);

        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
            log.debug("Generated new Correlation-Id '{}' for request path: {}", correlationId, request.getPath());
        } else {
            log.debug("Propagating existing Correlation-Id '{}' for request path: {}", correlationId, request.getPath());
        }

        final String finalCorrelationId = correlationId;

        // Mutate incoming request with the correlation ID header
        ServerHttpRequest mutatedRequest = request.mutate()
            .header(CORRELATION_ID_HEADER, finalCorrelationId)
            .build();

        ServerWebExchange mutatedExchange = exchange.mutate()
            .request(mutatedRequest)
            .build();

        // Mutate response to include the correlation ID if not already committed
        mutatedExchange.getResponse().beforeCommit(() -> {
            if (!mutatedExchange.getResponse().getHeaders().containsKey(CORRELATION_ID_HEADER)) {
                mutatedExchange.getResponse().getHeaders().set(CORRELATION_ID_HEADER, finalCorrelationId);
            }
            return Mono.empty();
        });

        return next.apply(mutatedExchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
