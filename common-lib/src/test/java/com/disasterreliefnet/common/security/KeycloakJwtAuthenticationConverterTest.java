package com.disasterreliefnet.common.security;

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

class KeycloakJwtAuthenticationConverterTest {

    private final KeycloakJwtAuthenticationConverter converter = new KeycloakJwtAuthenticationConverter();

    @Test
    void shouldExtractRealmRolesAndMapToGrantedAuthorities() {
        Jwt jwt = Jwt.withTokenValue("mock.jwt.token")
            .header("alg", "RS256")
            .claim("sub", "user-uuid-123")
            .claim("preferred_username", "relief_coordinator")
            .claim("realm_access", Map.of("roles", List.of("ADMIN", "ROLE_COORDINATOR", "DONOR")))
            .claim("resource_access", Map.of(
                "disaster-relief-client", Map.of("roles", List.of("DRIVER"))
            ))
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();

        AbstractAuthenticationToken authentication = converter.convert(jwt);

        assertNotNull(authentication);
        assertEquals("relief_coordinator", authentication.getName());

        Collection<GrantedAuthority> authorities = authentication.getAuthorities();
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

    @Test
    void shouldHandleJwtWithoutRolesGracefully() {
        Jwt jwt = Jwt.withTokenValue("mock.jwt.token")
            .header("alg", "RS256")
            .claim("sub", "user-uuid-456")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();

        AbstractAuthenticationToken authentication = converter.convert(jwt);
        assertNotNull(authentication);
        assertEquals("user-uuid-456", authentication.getName());
    }
}
