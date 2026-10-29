package com.disasterreliefnet.gateway.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Instant;

/**
 * Controller providing RFC 7807 ProblemDetail responses when a downstream microservice circuit breaker trips.
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    private static final Logger log = LoggerFactory.getLogger(FallbackController.class);

    @RequestMapping("/{service}")
    public Mono<ResponseEntity<ProblemDetail>> handleServiceFallback(@PathVariable("service") String service) {
        log.warn("Circuit breaker fallback triggered for downstream service: {}", service);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.SERVICE_UNAVAILABLE,
            String.format("The downstream service '%s' is temporarily unavailable or degraded. Please try again shortly.", service)
        );
        problem.setTitle("Service Temporarily Unavailable");
        problem.setType(URI.create("https://api.disasterreliefnet.org/errors/service-unavailable"));
        problem.setProperty("service", service);
        problem.setProperty("timestamp", Instant.now());

        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(problem));
    }

    @RequestMapping
    public Mono<ResponseEntity<ProblemDetail>> handleDefaultFallback() {
        return handleServiceFallback("unknown-service");
    }
}
