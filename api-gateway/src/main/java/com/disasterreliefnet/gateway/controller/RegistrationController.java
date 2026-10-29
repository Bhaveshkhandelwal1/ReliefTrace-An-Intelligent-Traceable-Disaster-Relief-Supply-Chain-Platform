package com.disasterreliefnet.gateway.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Handles self-registration with role assignment via Keycloak Admin REST API.
 * Exposed at POST /api/v1/auth/register (public — no JWT required).
 */
@RestController
@RequestMapping("/api/v1/auth")
public class RegistrationController {

    @Value("${keycloak.admin-url:http://drn-keycloak:8080}")
    private String keycloakAdminUrl;

    @Value("${keycloak.realm:disaster-relief-realm}")
    private String realm;

    @Value("${keycloak.admin-client-id:admin-cli}")
    private String adminClientId;

    @Value("${keycloak.admin-username:admin}")
    private String adminUsername;

    @Value("${keycloak.admin-password:admin}")
    private String adminPassword;

    private final WebClient webClient;

    // Allowed self-registration roles (ADMIN/COORDINATOR only via admin panel)
    private static final List<String> ALLOWED_ROLES = List.of("DONOR", "RECEIVER", "DRIVER");

    public RegistrationController(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public record RegisterRequest(
        String firstName,
        String lastName,
        String email,
        String username,
        String password,
        String role
    ) {}

    public record RegisterResponse(String message, boolean success) {}

    @PostMapping("/register")
    public Mono<ResponseEntity<RegisterResponse>> register(@RequestBody RegisterRequest req) {

        if (req.role() == null || !ALLOWED_ROLES.contains(req.role().toUpperCase())) {
            return Mono.just(ResponseEntity.badRequest()
                .body(new RegisterResponse("Invalid role. Must be one of: DONOR, RECEIVER, DRIVER", false)));
        }

        String normalizedRole = req.role().toUpperCase();

        return getAdminToken()
            .flatMap(token -> createUser(token, req)
                .flatMap(userId -> assignRole(token, userId, normalizedRole))
                .then(Mono.just(ResponseEntity.ok(
                    new RegisterResponse("Account created. Please login with your credentials.", true)
                )))
            )
            .onErrorResume(ex -> {
                String msg = ex.getMessage() != null ? ex.getMessage() : "Registration failed";
                return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new RegisterResponse(msg, false)));
            });
    }

    /** Obtain an admin access token from Keycloak master realm */
    private Mono<String> getAdminToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", adminClientId);
        form.add("username", adminUsername);
        form.add("password", adminPassword);
        form.add("grant_type", "password");

        return webClient.post()
            .uri(keycloakAdminUrl + "/realms/master/protocol/openid-connect/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromFormData(form))
            .retrieve()
            .bodyToMono(Map.class)
            .map(body -> (String) body.get("access_token"));
    }

    /** Create user in Keycloak, return the new user's UUID */
    @SuppressWarnings("unchecked")
    private Mono<String> createUser(String token, RegisterRequest req) {
        Map<String, Object> userRep = Map.of(
            "firstName", req.firstName(),
            "lastName",  req.lastName(),
            "email",     req.email(),
            "username",  req.username() != null ? req.username() : req.email(),
            "enabled",   true,
            "emailVerified", true,
            "credentials", List.of(Map.of(
                "type",      "password",
                "value",     req.password(),
                "temporary", false
            ))
        );

        return webClient.post()
            .uri(keycloakAdminUrl + "/admin/realms/" + realm + "/users")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(userRep)
            .retrieve()
            .toBodilessEntity()
            .flatMap(resp -> {
                // 201 Created — Location header contains the user URL with UUID
                String location = resp.getHeaders().getFirst(HttpHeaders.LOCATION);
                if (location == null) {
                    return Mono.error(new RuntimeException("User created but no location header returned"));
                }
                String userId = location.substring(location.lastIndexOf('/') + 1);
                return Mono.just(userId);
            })
            .onErrorMap(ex -> {
                if (ex.getMessage() != null && ex.getMessage().contains("409")) {
                    return new RuntimeException("Email or username already exists");
                }
                return ex;
            });
    }

    /** Fetch the role representation and assign it to the user */
    @SuppressWarnings("unchecked")
    private Mono<Void> assignRole(String token, String userId, String roleName) {
        String rolesUrl = keycloakAdminUrl + "/admin/realms/" + realm + "/roles/" + roleName;

        return webClient.get()
            .uri(rolesUrl)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .retrieve()
            .bodyToMono(Map.class)
            .flatMap(roleRep -> webClient.post()
                .uri(keycloakAdminUrl + "/admin/realms/" + realm + "/users/" + userId + "/role-mappings/realm")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(List.of(roleRep))
                .retrieve()
                .toBodilessEntity()
                .then()
            );
    }
}
