package com.disasterreliefnet.common.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Converts Keycloak JWT tokens into Spring Security Authentication objects,
 * extracting realm_access and resource_access roles and mapping them to Spring GrantedAuthority (ROLE_*).
 */
public class KeycloakJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final JwtGrantedAuthoritiesConverter defaultAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = Stream.concat(
            defaultAuthoritiesConverter.convert(jwt).stream(),
            extractKeycloakRoles(jwt).stream()
        ).collect(Collectors.toSet());

        String principalClaimName = getPrincipalClaimName(jwt);
        return new JwtAuthenticationToken(jwt, authorities, principalClaimName);
    }

    @SuppressWarnings("unchecked")
    public Collection<GrantedAuthority> extractKeycloakRoles(Jwt jwt) {
        Set<GrantedAuthority> authorities = new HashSet<>();

        // 1. Extract Realm Roles
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null && realmAccess.containsKey("roles")) {
            Collection<String> roles = (Collection<String>) realmAccess.get("roles");
            if (roles != null) {
                for (String role : roles) {
                    addRoleAuthorities(authorities, role);
                }
            }
        }

        // 2. Extract Resource Access (Client) Roles
        Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
        if (resourceAccess != null) {
            for (Map.Entry<String, Object> clientEntry : resourceAccess.entrySet()) {
                if (clientEntry.getValue() instanceof Map<?, ?> clientMap) {
                    Object clientRolesObj = clientMap.get("roles");
                    if (clientRolesObj instanceof Collection<?> clientRoles) {
                        for (Object r : clientRoles) {
                            if (r instanceof String roleStr) {
                                addRoleAuthorities(authorities, roleStr);
                            }
                        }
                    }
                }
            }
        }

        return authorities;
    }

    private void addRoleAuthorities(Set<GrantedAuthority> authorities, String role) {
        if (role == null || role.isBlank()) {
            return;
        }
        String cleanRole = role.trim();
        if (cleanRole.startsWith("ROLE_")) {
            authorities.add(new SimpleGrantedAuthority(cleanRole));
            authorities.add(new SimpleGrantedAuthority(cleanRole.substring(5)));
        } else {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + cleanRole));
            authorities.add(new SimpleGrantedAuthority(cleanRole));
        }
    }

    private String getPrincipalClaimName(Jwt jwt) {
        if (jwt.hasClaim("preferred_username")) {
            return jwt.getClaimAsString("preferred_username");
        }
        if (jwt.hasClaim("email")) {
            return jwt.getClaimAsString("email");
        }
        return jwt.getSubject();
    }
}
