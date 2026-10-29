package com.disasterreliefnet.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.util.Optional;

/**
 * Key resolver configuration for Redis token bucket rate limiting.
 * Resolves to the authenticated user principal name if available, or client remote IP for anonymous calls.
 */
@Configuration
public class RateLimiterConfig {

    @Bean
    @Primary
    public KeyResolver userKeyResolver() {
        return exchange -> exchange.getPrincipal()
            .map(Principal::getName)
            .switchIfEmpty(Mono.defer(() -> {
                String ip = Optional.ofNullable(exchange.getRequest().getRemoteAddress())
                    .map(addr -> addr.getAddress().getHostAddress())
                    .orElse("anonymous");
                return Mono.just("ip:" + ip);
            }));
    }
}
