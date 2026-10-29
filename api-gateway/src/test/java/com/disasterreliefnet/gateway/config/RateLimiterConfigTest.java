package com.disasterreliefnet.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import java.net.InetSocketAddress;
import java.security.Principal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RateLimiterConfigTest {

    private final RateLimiterConfig config = new RateLimiterConfig();

    @Test
    void shouldResolveAuthenticatedPrincipalName() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/donors").build();
        Principal principal = () -> "user-uuid-12345";
        org.springframework.web.server.ServerWebExchange exchange = MockServerWebExchange.from(request)
            .mutate()
            .principal(reactor.core.publisher.Mono.just(principal))
            .build();

        String key = config.userKeyResolver().resolve(exchange).block();
        assertEquals("user-uuid-12345", key);
    }

    @Test
    void shouldResolveClientIpWhenUnauthenticated() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/donors")
            .remoteAddress(new InetSocketAddress("192.168.1.100", 5050))
            .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        String key = config.userKeyResolver().resolve(exchange).block();
        assertEquals("ip:192.168.1.100", key);
    }
}
