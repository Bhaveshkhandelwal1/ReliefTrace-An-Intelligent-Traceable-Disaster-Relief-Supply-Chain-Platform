package com.disasterreliefnet.gateway.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class KeycloakReactiveJwtAuthenticationConverterTest {

    private final KeycloakReactiveJwtAuthenticationConverter converter = new KeycloakReactiveJwtAuthenticationConverter();

    @Test
    void shouldExtractRealmRolesAndAuthoritiesReactively() {
        Jwt jwt = Jwt.withTokenValue("mock.gateway.token")
            .header("alg", "RS256")
            .claim("sub", "gateway-user-123")
            .claim("preferred_username", "relief_coordinator")
            .claim("realm_access", Map.of("roles", List.of("ADMIN", "ROLE_COORDINATOR", "DONOR")))
            .claim("resource_access", Map.of(
                "disaster-relief-client", Map.of("roles", List.of("DRIVER"))
            ))
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();

        AbstractAuthenticationToken token = converter.convert(jwt).block();

        assertNotNull(token);
        assertEquals("relief_coordinator", token.getName());

        Collection<GrantedAuthority> authorities = token.getAuthorities();
        Set<String> authorityNames = authorities.stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toSet());

        assertTrue(authorityNames.contains("ROLE_ADMIN"));
        assertTrue(authorityNames.contains("ADMIN"));
        assertTrue(authorityNames.contains("ROLE_COORDINATOR"));
        assertTrue(authorityNames.contains("COORDINATOR"));
        assertTrue(authorityNames.contains("ROLE_DONOR"));
        assertTrue(authorityNames.contains("DONOR"));
        assertTrue(authorityNames.contains("ROLE_DRIVER"));
        assertTrue(authorityNames.contains("DRIVER"));
    }
}
