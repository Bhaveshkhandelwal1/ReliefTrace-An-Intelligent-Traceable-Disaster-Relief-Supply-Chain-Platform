# DisasterReliefNet

[![Java 21](https://img.shields.io/badge/Java-21-blue.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot 3.3.4](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Cloud 2023.0.3](https://img.shields.io/badge/Spring%20Cloud-2023.0.3-green.svg)](https://spring.io/projects/spring-cloud)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

DisasterReliefNet is a cloud-native, event-driven microservices platform designed to coordinate humanitarian disaster relief supply chains. It connects donors, disaster victims, relief organizations, and transport providers in real-time.

---

## 1. System Architecture

```
                       [ Keycloak (Port 8484) ]
                                  │ (JWT / OIDC)
                                  ▼
[ Public Clients / Web ] ──► [ API Gateway (Port 8080) ]
                                  │ (lb:// routing)
           ┌──────────────────────┼──────────────────────┐
           ▼                      ▼                      ▼
  [ Donor Service ]     [ Receiver Service ]   [ Inventory Service ]
      (Port 8081)           (Port 8082)            (Port 8083)
           │                      │                      │
           └──────────────────────┼──────────────────────┘
                                  ▼
                     [ Kafka Event Bus (Port 9092) ]
                                  ▲
           ┌──────────────────────┼──────────────────────┐
           ▼                      ▼                      ▼
  [ Matching Service ]   [ Transport Service ]  [ Finance Service ]
      (Port 8084)            (Port 8085)            (Port 8089)
```

### Module Overview

| Module | Port | Database | Description |
|---|---|---|---|
| [`discovery-server`](discovery-server) | `8761` | — | Netflix Eureka Service Registry |
| [`api-gateway`](api-gateway) | `8080` | — | Spring Cloud Gateway, JWT Token Validation, Rate Limiter |
| [`common-lib`](common-lib) | — | — | Shared RFC 7807 Exception Handlers, Enums, DTOs, EventEnvelope |
| [`donor-service`](donor-service) | `8081` | `donor_db` | Donor registration and monetary / in-kind donations |
| [`receiver-service`](receiver-service) | `8082` | `receiver_db` | Aid beneficiary intake and relief requests |
| [`inventory-service`](inventory-service) | `8083` | `inventory_db` | Warehouse catalog, supplies tracking, stock reservations |
| [`matching-service`](matching-service) | `8084` | `matching_db` | Automated request-to-inventory matching algorithms |
| [`transport-service`](transport-service) | `8085` | `transport_db` | Driver dispatch, route monitoring, and delivery tracking |
| [`verification-service`](verification-service) | `8086` | `verification_db` | ID & document eligibility checks for recipients |
| [`disaster-service`](disaster-service) | `8087` | `disaster_db` | Disaster incidents declaration and severity status |
| [`notification-service`](notification-service) | `8088` | `notification_db` | Multi-channel messaging (Email, SMS, Push alerts) |
| [`finance-service`](finance-service) | `8089` | `finance_db` | Double-entry ledger, payments, driver disbursements |
| [`analytics-service`](analytics-service) | `8090` | `analytics_db` | Situation reports, fact aggregations, and metrics |

---

## 2. Prerequisites

- **Java JDK 21+** (Homebrew OpenJDK 21 or Eclipse Temurin 21)
- **Apache Maven 3.9+**
- **Docker & Docker Compose v2+**

---

## 3. Local Infrastructure Setup

DisasterReliefNet uses Docker Compose to run backing infrastructure services locally:
- **PostgreSQL 16**: Preloaded with 10 isolated databases + Keycloak database via `docker/postgres/init.sql`.
- **Apache Kafka 3.7**: Operating in **KRaft** mode (no ZooKeeper required).
- **Redis 7**: For gateway rate limiting and distributed caching.
- **Keycloak 24**: Identity and Access Management pre-configured with the `disaster-relief-realm` and 5 standard roles.
- **Prometheus & Grafana**: Pre-configured monitoring and metric scraping.

### Start Backing Services

```bash
# Using Makefile
make up

# Or directly using Docker Compose
docker compose up -d
```

### Backing Service Consoles & Credentials

| Service | URL | Credentials |
|---|---|---|
| **PostgreSQL** | `localhost:5432` | `postgres` / `postgres` |
| **Kafka Broker** | `localhost:9092` (internal), `localhost:9094` (external) | — |
| **Redis** | `localhost:6379` | — |
| **Keycloak Admin** | [http://localhost:8484](http://localhost:8484) | `admin` / `admin` |
| **Eureka Discovery** | [http://localhost:8761](http://localhost:8761) | `eureka` / `eureka123` |
| **Prometheus** | [http://localhost:9090](http://localhost:9090) | — |
| **Grafana** | [http://localhost:3000](http://localhost:3000) | `admin` / `admin` |

### Preloaded Keycloak Sample Users

All sample accounts are preloaded into `disaster-relief-realm` with password `password`:

| Username | Role | Role Description |
|---|---|---|
| `admin` | `ADMIN`, `ROLE_ADMIN` | Platform administrator |
| `coordinator` | `COORDINATOR`, `ROLE_COORDINATOR` | Relief organization staff |
| `donor` | `DONOR`, `ROLE_DONOR` | Monetary & in-kind donor |
| `receiver` | `RECEIVER`, `ROLE_RECEIVER` | Beneficiary / aid applicant |
| `driver` | `DRIVER`, `ROLE_DRIVER` | Transport driver |

---

## 4. Build and Test

```bash
# Run full clean build with all tests and JaCoCo coverage analysis
make test
# OR
mvn clean verify

# Fast build skipping tests
make build
# OR
mvn clean package -DskipTests
```

---

## 5. Running the Microservices

Start the services in the designated build order:

```bash
# 1. Start Eureka Discovery Server
mvn spring-boot:run -pl discovery-server

# 2. Start API Gateway
mvn spring-boot:run -pl api-gateway

# 3. Start Core Domain Services (in separate terminal tabs or background)
mvn spring-boot:run -pl disaster-service
mvn spring-boot:run -pl donor-service
mvn spring-boot:run -pl receiver-service
mvn spring-boot:run -pl verification-service
mvn spring-boot:run -pl inventory-service
mvn spring-boot:run -pl matching-service
mvn spring-boot:run -pl transport-service
mvn spring-boot:run -pl notification-service
mvn spring-boot:run -pl finance-service
mvn spring-boot:run -pl analytics-service
```

---

## 6. Stop Infrastructure

```bash
make down
# OR
docker compose down -v
```
# ReliefTrace-An-Intelligent-Traceable-Disaster-Relief-Supply-Chain-Platform
