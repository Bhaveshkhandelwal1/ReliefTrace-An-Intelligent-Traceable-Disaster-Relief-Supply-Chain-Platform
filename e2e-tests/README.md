# DisasterReliefNet :: End-to-End Saga & Failure Integration Tests (`e2e-tests`)

This module provides comprehensive end-to-end (E2E) integration tests for the DisasterReliefNet microservices platform using **Testcontainers**, **REST-Assured**, **Awaitility**, and direct database/Kafka assertions.

---

## 1. Architecture & Test Scope

The E2E test suite boots the complete microservices architecture in isolated Docker containers:
* **Kafka Container** (Single broker with dynamic topics)
* **Redis Container** (Shared cache)
* **8 PostgreSQL Database Containers** (One dedicated DB container per microservice)
* **8 Microservice GenericContainers**:
  * `disaster-service` (Port 8087)
  * `donor-service` (Port 8081)
  * `receiver-service` (Port 8082)
  * `verification-service` (Port 8086)
  * `inventory-service` (Port 8083)
  * `matching-service` (Port 8084)
  * `transport-service` (Port 8085)
  * `finance-service` (Port 8089)
  * `notification-service` (Port 8088)

### Test Suites Included:
1. **Full Happy-Path Saga (`FullSagaIT`)**:
   - Step 1: Declare disaster
   - Step 2: Donor registers and submits 1,000 kg in-kind food donation (`DonationRegistered` Kafka event)
   - Step 3: Inventory warehouse created and stocked with 1,000 kg
   - Step 4: Two camps submit aid requests (`CRITICAL` urgency 300 kg and `HIGH` urgency 150 kg)
   - Step 5: Verification service approves the `CRITICAL` case (`RequestVerified` Kafka event)
   - Step 6: Matching service evaluates priorities; coordinator approves allocation (`AllocationApproved` Kafka event)
   - Step 7: Inventory reserves 300 kg (`StockReserved` Kafka event)
   - Step 8: Transport service creates shipment; coordinator assigns driver and vehicle (`ShipmentAssigned` Kafka event)
   - Step 9: Driver picks up goods (`PICKED_UP` status)
   - Step 10: Driver delivers goods (`DELIVERED` status, tracking events updated)
   - Step 11: Multi-service REST state verification

2. **Failure Scenarios**:
   - **`InsufficientStockCompensationIT`**:
     Inventory detects insufficient stock (requested 300 kg > available 50 kg) and emits `StockReservationFailed`. Matching service triggers saga compensation, marking allocation as `RESERVATION_FAILED` and reverting matched request to `PENDING`. Transport service creates no shipments.
   - **`DuplicateKafkaEventIT`**:
     Verifies idempotency in consumer execution by publishing duplicate `StockReserved` events with identical `eventId`. Verifies only 1 shipment is created and 1 entry exists in `processed_events`.
   - **`VehicleBreakdownReassignmentIT`**:
     Reports vehicle breakdown on active shipment via `POST /api/v1/transport/vehicles/{vehicleId}/breakdown`. Verifies vehicle deactivation, original driver release, automatic shipment reassignment to backup driver/vehicle, `REASSIGNED` tracking event recorded, and `ShipmentAssigned` Kafka event re-emitted.
   - **`NotificationServiceDownRetryIT`**:
     Verifies Transactional Outbox Pattern decoupling and consumer resilience. When notification service is stopped, donor registration succeeds, outbox publishes to Kafka, and upon notification service restart, all backlog events are consumed and recorded in `notification_logs` without data loss.

---

## 2. Prerequisites

1. **Docker / Colima / Rancher Desktop** running locally.
2. **Java 21** installed and configured (`java -version`).
3. **Maven Multi-Module Build**: Service Docker images must be built before executing the integration tests.

---

## 3. How to Run the Tests

### Step 1: Package the Microservices and Build Docker Images
```bash
# Package all services without running tests
mvn clean package -DskipTests
```

### Step 2: Run the E2E Test Suite
The integration tests use the `*IT.java` naming convention and are executed by `maven-failsafe-plugin`:

```bash
# Run all E2E integration tests
mvn verify -pl e2e-tests

# Run a specific IT test class
mvn verify -pl e2e-tests -Dit.test=FullSagaIT
mvn verify -pl e2e-tests -Dit.test=InsufficientStockCompensationIT
mvn verify -pl e2e-tests -Dit.test=DuplicateKafkaEventIT
mvn verify -pl e2e-tests -Dit.test=VehicleBreakdownReassignmentIT
mvn verify -pl e2e-tests -Dit.test=NotificationServiceDownRetryIT
```

### Skipping E2E Tests During Regular Builds
Unit tests (`mvn test`) run with `maven-surefire-plugin` and will skip the integration tests. To run `mvn verify` across the entire project while skipping the containerized E2E tests:
```bash
mvn verify -DskipITs
```
