package com.disasterreliefnet.gateway.config;

import com.disasterreliefnet.gateway.security.KeycloakReactiveJwtAuthenticationConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Security configuration for the Reactive API Gateway.
 * Enforces role-based path permissions, CORS policies, and Keycloak JWT validation.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private final KeycloakReactiveJwtAuthenticationConverter jwtAuthenticationConverter;

    @Value("${cors.allowed-origins:http://localhost:5173,http://localhost:3000}")
    private String allowedOrigins;

    public SecurityConfig(KeycloakReactiveJwtAuthenticationConverter jwtAuthenticationConverter) {
        this.jwtAuthenticationConverter = jwtAuthenticationConverter;
    }

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeExchange(exchanges -> exchanges
                // Public Self-Registration (no JWT required)
                .pathMatchers(HttpMethod.POST, "/api/v1/auth/register").permitAll()

                // Public Actuator, Fallback & Swagger endpoints
                .pathMatchers("/actuator/**").permitAll()
                .pathMatchers("/fallback/**").permitAll()
                .pathMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/webjars/**").permitAll()

                // Public Self-Registration & Disaster Alerts
                .pathMatchers(HttpMethod.POST, "/api/v1/donors").permitAll()
                .pathMatchers(HttpMethod.POST, "/api/v1/receivers").permitAll()
                .pathMatchers(HttpMethod.GET, "/api/v1/disasters/**").permitAll()

                // Domain Service Role-Protected Endpoints
                .pathMatchers("/api/v1/donors/**").hasAnyRole("ADMIN", "COORDINATOR", "DONOR")
                .pathMatchers("/api/v1/receivers/**").hasAnyRole("ADMIN", "COORDINATOR", "RECEIVER")
                .pathMatchers("/api/v1/verification/**").hasAnyRole("ADMIN", "COORDINATOR", "RECEIVER")
                .pathMatchers("/api/v1/inventory/**").hasAnyRole("ADMIN", "COORDINATOR", "DRIVER")
                .pathMatchers("/api/v1/matching/**").hasAnyRole("ADMIN", "COORDINATOR")
                .pathMatchers("/api/v1/transport/**").hasAnyRole("ADMIN", "COORDINATOR", "DRIVER", "RECEIVER")
                .pathMatchers(HttpMethod.POST, "/api/v1/disasters/**").hasAnyRole("ADMIN", "COORDINATOR")
                .pathMatchers(HttpMethod.PUT, "/api/v1/disasters/**").hasAnyRole("ADMIN", "COORDINATOR")
                .pathMatchers(HttpMethod.DELETE, "/api/v1/disasters/**").hasAnyRole("ADMIN", "COORDINATOR")
                .pathMatchers(HttpMethod.PATCH, "/api/v1/disasters/**").hasAnyRole("ADMIN", "COORDINATOR")
                .pathMatchers("/api/v1/notifications/**").hasAnyRole("ADMIN", "COORDINATOR", "DONOR", "RECEIVER", "DRIVER")
                .pathMatchers("/api/v1/finance/**").hasAnyRole("ADMIN", "COORDINATOR", "DONOR")
                .pathMatchers("/api/v1/analytics/**").hasAnyRole("ADMIN", "COORDINATOR")

                // All other requests require authentication
                .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
            );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .toList();

        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
