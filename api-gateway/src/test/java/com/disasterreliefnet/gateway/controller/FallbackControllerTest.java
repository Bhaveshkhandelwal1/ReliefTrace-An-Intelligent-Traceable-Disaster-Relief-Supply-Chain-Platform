package com.disasterreliefnet.gateway.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class FallbackControllerTest {

    private final FallbackController controller = new FallbackController();

    @Test
    void shouldReturnProblemDetailWithServiceUnavailableStatus() {
        ResponseEntity<ProblemDetail> response = controller.handleServiceFallback("donor-service").block();

        assertNotNull(response);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());

        ProblemDetail problem = response.getBody();
        assertNotNull(problem);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE.value(), problem.getStatus());
        assertEquals("Service Temporarily Unavailable", problem.getTitle());
        assertTrue(problem.getDetail().contains("donor-service"));
        assertEquals("donor-service", problem.getProperties().get("service"));
        assertNotNull(problem.getProperties().get("timestamp"));
    }

    @Test
    void shouldHandleDefaultFallbackGracefully() {
        ResponseEntity<ProblemDetail> response = controller.handleDefaultFallback().block();

        assertNotNull(response);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals("unknown-service", response.getBody().getProperties().get("service"));
    }
}
