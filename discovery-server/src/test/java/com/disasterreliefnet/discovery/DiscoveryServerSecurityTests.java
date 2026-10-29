package com.disasterreliefnet.discovery;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DiscoveryServerSecurityTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldAllowUnauthenticatedAccessToActuatorHealth() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void shouldDenyUnauthenticatedAccessToEurekaEndpoints() {
        ResponseEntity<String> response = restTemplate.getForEntity("/eureka/apps", String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void shouldAllowAuthenticatedAccessWithEurekaCredentials() {
        ResponseEntity<String> response = restTemplate.withBasicAuth("eureka", "eureka123")
            .getForEntity("/eureka/apps", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
