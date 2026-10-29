# DisasterReliefNet :: Enterprise Security Architecture Review

**Author:** Senior Java Backend Architect  
**Classification:** Confidential / Internal Security Baseline  
**Scope:** DisasterReliefNet Microservices Platform (Java 21, Spring Boot 3.3.x, Spring Cloud 2023.0.x, Keycloak, Kafka, PostgreSQL, Redis)

---

## 1. Executive Summary

DisasterReliefNet handles humanitarian emergency response operations, coordinating donors, victim aid requests, financial disbursements, and relief goods transport. Because malicious actors or system corruption during a disaster could disrupt life-saving operations, the platform implements a **Defense-in-Depth (DiD)** security architecture adhering to the **OWASP Top 10 (2021)** and **NIST Cybersecurity Framework**.

### Core Tenets of the Security Posture:
1. **Zero-Trust Network Model**: Services assume all network traffic (internal and external) is untrusted until authenticated and authorized.
2. **Strict Perimeter Validation**: All payloads crossing service boundaries are strictly validated before business processing.
3. **Identity & Least Privilege**: Fine-grained Role-Based Access Control (RBAC) enforced via Keycloak JWTs and Spring Method Security.
4. **Data Isolation & Confidentiality**: Database-per-service isolation; zero cross-database queries; secrets externalized with zero hardcoded values.
5. **Traceability & Non-Repudiation**: Correlation IDs (`X-Correlation-ID`) across HTTP and Kafka; immutable transactional outbox audit trails.

---

## 2. Threat Vector Analysis & Mitigation Matrix (OWASP Top 10)

| OWASP Vulnerability | Threat Scenario in DisasterReliefNet | Implemented Mitigation & Verification |
|---|---|---|
| **A01: Broken Access Control** | An unverified user or malicious driver attempts to modify an approved allocation or alter shipment delivery states. | Keycloak OIDC/JWT integration; `@PreAuthorize("hasRole('ADMIN') or hasRole('COORDINATOR')")` on sensitive mutating endpoints; ownership checks in domain services (e.g., verifying `driverId` matches authenticated subject). |
| **A02: Cryptographic Failures** | Eavesdropping on aid delivery coordinates, victim PII, or donor payment details in transit. | TLS 1.3 enforced at Ingress and API Gateway; sensitive PII fields (phone, address, tax IDs) encrypted at rest in PostgreSQL via `pgcrypto` extensions; bcrypt/Argon2 password hashing in Keycloak. |
| **A03: Injection** | SQL injection in search/filter endpoints; command injection in file/export routines. | JPA/Hibernate parameterized queries only; zero raw SQL concatenation; DTO records enforce `@NotBlank`, `@Size`, `@Pattern`, and Jakarta Bean Validation. |
| **A04: Insecure Design** | Unlimited request creation causing resource exhaustion; negative inventory allocations. | Redis Token Bucket rate limiting in Spring Cloud Gateway; domain invariant guards in services (e.g., allocations cannot exceed verified request quantities or warehouse inventory; `@DecimalMin("0.01")`). |
| **A05: Security Misconfiguration** | Default credentials; framework stack traces leaked in error responses; debug endpoints exposed. | Actuator endpoints restricted to `health, info, prometheus, metrics`; centralized `@RestControllerAdvice` returning sanitized RFC 7807 `ProblemDetail` without stack traces; `hibernate.ddl-auto=validate`. |
| **A06: Vulnerable & Outdated Components** | Exploits in third-party libraries (e.g. log4j, Jackson vulnerabilities). | Automated OWASP Dependency-Check integrated into Maven build and GitHub Actions; Trivy container image scanning; automated Dependabot dependency updates. |
| **A07: Identification & Auth Failures** | Brute force login attacks against field workers; session hijacking. | Centralized Keycloak IdP with brute-force detection, token expiration (15-min access tokens, refresh tokens), and cryptographic signature validation (`RS256`). |
| **A08: Software & Data Integrity Failures** | Tampering with Kafka event envelopes in transit or duplicate message replay. | Cryptographic UUID `eventId` tracking via `processed_events` table (idempotent consumer pattern); transactional outbox ensures atomicity of DB writes and event emissions. |
| **A09: Security Logging & Monitoring Failures** | Unauthorized data access goes undetected during high-volume disaster operations. | Centralized MDC structured JSON logging with `correlationId` and OpenTelemetry `traceId`; Prometheus scrape metrics and alerts for 5xx errors and circuit breaker state changes; Grafana visualization. |
| **A10: Server-Side Request Forgery (SSRF)** | Feign clients tricked into querying internal cloud metadata endpoints (e.g. AWS 169.254.169.254). | All Feign client targets are statically configured via Spring properties; strict URI path sanitization; Kubernetes NetworkPolicies block pod egress to cloud metadata services. |

---

## 3. Input Validation & Data Sanitization

### 3.1 DTO Record Immutability & Jakarta Validation
Entities are never exposed via REST controllers. Every request is mapped through an immutable Java record with Jakarta validation annotations:

```java
public record CreateAidRequest(
    @NotNull UUID disasterId,
    @NotNull AidCategory category,
    @NotBlank @Size(max = 500) String description,
    @NotNull @DecimalMin(value = "0.01", message = "Requested quantity must be positive") BigDecimal quantityRequested,
    @NotBlank @Size(max = 20) String unit,
    @NotNull UrgencyLevel urgencyLevel,
    @NotNull @Valid DeliveryAddressDto deliveryAddress
) {}
```

### 3.2 RFC 7807 Problem Details Sanitization
The platform uses a centralized `GlobalExceptionHandler` returning RFC 7807 `ProblemDetail`. Internal exceptions (e.g., `SQLException`, `DataAccessException`, stack traces) are logged to SLF4J at `ERROR` level but **never returned to the client**.

Example client response:
```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Requested quantity must be positive",
  "instance": "/api/v1/receivers/req-123/requests",
  "timestamp": "2026-10-29T18:45:00Z"
}
```

---

## 4. Authentication & Authorization Architecture

### 4.1 Keycloak OAuth2 / OIDC Integration
1. Clients authenticate against Keycloak (`/realms/disaster-relief-realm`).
2. Gateway and Resource Services validate JWT signatures via JWKS (`KEYCLOAK_JWK_SET_URI`).
3. `KeycloakJwtAuthenticationConverter` maps Realm and Resource roles from `realm_access.roles` into Spring Security `GrantedAuthority` objects (`ROLE_ADMIN`, `ROLE_COORDINATOR`, `ROLE_DONOR`, `ROLE_RECEIVER`, `ROLE_DRIVER`).

### 4.2 Method-Level Security
Sensitive business mutations enforce role requirements:
```java
@PostMapping("/{vehicleId}/breakdown")
@PreAuthorize("hasAnyRole('DRIVER', 'ADMIN', 'COORDINATOR')")
public ResponseEntity<VehicleResponse> reportBreakdown(
    @PathVariable UUID vehicleId,
    @Valid @RequestBody BreakdownRequest request
) { ... }
```

---

## 5. Secrets Management & Environment Isolation

1. **Zero Hardcoded Credentials**:
   - All connection strings, passwords, and API tokens use environment variable substitution with sensible fallback defaults for isolated local development:
     ```yaml
     spring:
       datasource:
         url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/donor_db}
         username: ${SPRING_DATASOURCE_USERNAME:postgres}
         password: ${SPRING_DATASOURCE_PASSWORD:postgres}
     ```
2. **Kubernetes Secrets & HashiCorp Vault Integration**:
   - Production secrets are injected into pods via Kubernetes `Secret` resources (`drn-secrets`).
   - Passwords for PostgreSQL, Keycloak Admin, and SSL keys are mounted as environment variables from secret keys.
   - Files containing secrets are excluded via `.gitignore`.

---

## 6. Network Segmentation & Perimeter Defense

### 6.1 Zero-Trust Kubernetes Network Policies
Under `k8s/02-network-policies.yaml`:
- **Default Deny All**: All pod ingress and egress traffic is dropped by default.
- **Gateway Isolation**: Only the NGINX Ingress controller can communicate with `api-gateway` on port 8080.
- **Database Isolation**: PostgreSQL (port 5432) only accepts connections originating from domain service pods (`tier: domain`).
- **Kafka & Redis Isolation**: Kafka and Redis accept traffic exclusively from platform microservices.

### 6.2 Rate Limiting & Denial-of-Service Defense
- Spring Cloud Gateway utilizes Redis-backed Token Bucket rate limiting:
  - 100 requests/second with a burst capacity of 200 per authenticated client ID.
  - Anonymous traffic throttled to 20 requests/second per IP address.

---

## 7. Supply Chain & Dependency Security

1. **OWASP Dependency-Check**:
   - Configured in the CI/CD pipeline (`.github/workflows/ci-cd.yml`).
   - Builds fail on any detected dependency vulnerability with a CVSS score $\ge 8.0$.
2. **Container Image Scanning (Trivy)**:
   - Scans Docker base images and operating system dependencies for CVEs.
   - Automated SARIF reporting uploaded directly to GitHub Security Advisories.
3. **ArchUnit Enforced Boundaries**:
   - Unit tests enforce that domain models never import web/controller classes, JPA entities are never exposed across modules, and layered architecture boundaries are strictly respected.

---

## 8. Auditability & Observability

1. **Trace Correlation**:
   - `CorrelationIdFilter` inspects incoming HTTP requests for `X-Correlation-ID`.
   - Propagated to SLF4J MDC, Kafka event headers, and downstream Feign calls.
2. **Structured JSON Logging**:
   - `LogstashEncoder` produces structured JSON log lines containing `service`, `correlationId`, `traceId`, and `spanId` for ingestion into Elasticsearch/Grafana Loki.
3. **OpenTelemetry Distributed Tracing**:
   - Tracing spans forwarded to Zipkin/Tempo (`http://zipkin:9411/api/v2/spans`).
   - Spans record HTTP RPC calls, database queries, and Kafka message lifecycles.
