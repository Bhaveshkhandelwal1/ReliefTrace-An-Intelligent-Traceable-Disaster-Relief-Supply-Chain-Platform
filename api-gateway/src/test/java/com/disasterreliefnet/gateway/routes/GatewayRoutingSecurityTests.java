package com.disasterreliefnet.gateway.routes;

import com.disasterreliefnet.gateway.filter.CorrelationIdFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=6379"
    }
)
@AutoConfigureWebTestClient
class GatewayRoutingSecurityTests {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ReactiveJwtDecoder reactiveJwtDecoder;

    @Test
    void shouldAllowPublicAccessToFallbackEndpoint() {
        webTestClient.get()
            .uri("/fallback/inventory-service")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
            .expectHeader().exists(CorrelationIdFilter.CORRELATION_ID_HEADER)
            .expectBody()
            .jsonPath("$.status").isEqualTo(503)
            .jsonPath("$.title").isEqualTo("Service Temporarily Unavailable")
            .jsonPath("$.service").isEqualTo("inventory-service");
    }

    @Test
    void shouldDenyUnauthenticatedAccessToProtectedRoutes() {
        webTestClient.get()
            .uri("/api/v1/donors/12345")
            .exchange()
            .expectStatus().isUnauthorized();
    }

    @Test
    void shouldDenyAccessWhenRoleIsForbiddenForEndpoint() {
        // DRIVER role does not have permission to view donor profiles
        webTestClient.mutateWith(
                mockJwt().authorities(new SimpleGrantedAuthority("ROLE_DRIVER"))
            )
            .get()
            .uri("/api/v1/donors/12345")
            .exchange()
            .expectStatus().isForbidden();
    }

    @Test
    void shouldAuthorizeAccessWhenRoleMatchesEndpointPermission() {
        // DONOR role has permission to access donor service endpoints
        webTestClient.mutateWith(
                mockJwt().authorities(new SimpleGrantedAuthority("ROLE_DONOR"))
            )
            .get()
            .uri("/api/v1/donors/12345")
            .exchange()
            // Even if downstream is not up, the status will be 503 (fallback) or 500, NOT 401 or 403
            .expectStatus().value(status -> {
                assert status != 401 && status != 403;
            });
    }

    @Test
    void shouldPermitPublicDonorRegistrationWithoutToken() {
        webTestClient.post()
            .uri("/api/v1/donors")
            .exchange()
            // Public endpoint: not 401 Unauthorized
            .expectStatus().value(status -> {
                assert status != 401;
            });
    }
}
