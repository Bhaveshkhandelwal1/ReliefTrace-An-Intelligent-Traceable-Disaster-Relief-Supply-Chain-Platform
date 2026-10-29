package com.disasterreliefnet.e2e.failure;

import com.disasterreliefnet.common.event.KafkaTopics;
import com.disasterreliefnet.e2e.infrastructure.BaseE2ETest;
import com.disasterreliefnet.e2e.infrastructure.KafkaEventCaptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Failure Scenario 3: Vehicle Breakdown and Automated Shipment Reassignment Test.
 *
 * <p>Flow:
 * <ol>
 *   <li>Register Primary Driver + Vehicle 1, and Backup Driver + Vehicle 2.</li>
 *   <li>Create a shipment and assign Primary Driver and Vehicle 1.</li>
 *   <li>Report vehicle breakdown via POST /api/v1/transport/vehicles/{vehicleId}/breakdown.</li>
 *   <li>Assert Vehicle 1 deactivated and Primary Driver freed.</li>
 *   <li>Assert Shipment automatically reassigned to Backup Driver and Vehicle 2.</li>
 *   <li>Assert tracking history records REASSIGNED event.</li>
 *   <li>Assert ShipmentAssigned Kafka event emitted for the reassignment.</li>
 * </ol>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("E2E Failure Scenario 3: Vehicle Breakdown and Automated Reassignment")
public class VehicleBreakdownReassignmentIT extends BaseE2ETest {

    private static final Logger log = LoggerFactory.getLogger(VehicleBreakdownReassignmentIT.class);

    private static String primaryDriverId;
    private static String primaryVehicleId;
    private static String backupDriverId;
    private static String backupVehicleId;
    private static String shipmentId;

    private static final UUID ALLOCATION_ID  = UUID.randomUUID();
    private static final UUID RESERVATION_ID = UUID.randomUUID();
    private static final UUID RECEIVER_ID    = UUID.randomUUID();

    @Test
    @Order(1)
    @DisplayName("Step 1: Register primary and backup drivers with their vehicles")
    void step1_registerFleet() {
        // 1a — Primary driver
        String primaryDriverBody = """
            {
              "userId":        "keycloak-driver-primary-%s",
              "firstName":     "Ajay",
              "lastName":      "Dev",
              "email":         "ajay.dev.%s@transport.org",
              "phone":         "+919811100001",
              "licenseNumber": "DL-AJAY-%s"
            }
            """.formatted(
                UUID.randomUUID().toString().substring(0, 8),
                UUID.randomUUID().toString().substring(0, 8),
                UUID.randomUUID().toString().substring(0, 6)
            );

        primaryDriverId = transport().when()
            .body(primaryDriverBody).post("/api/v1/transport/drivers")
            .then().statusCode(201)
            .extract().jsonPath().getString("driverId");

        // 1b — Primary vehicle
        String primaryPlate = "AS-01-BK-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        String primaryVehicleBody = """
            {
              "driverId":    "%s",
              "plateNumber": "%s",
              "type":        "LIGHT_TRUCK",
              "capacityKg":  2500.0,
              "capacityCbm": 15.0
            }
            """.formatted(primaryDriverId, primaryPlate);

        primaryVehicleId = transport().when()
            .body(primaryVehicleBody).post("/api/v1/transport/vehicles")
            .then().statusCode(201)
            .extract().jsonPath().getString("vehicleId");

        // 1c — Backup driver
        String backupDriverBody = """
            {
              "userId":        "keycloak-driver-backup-%s",
              "firstName":     "Manish",
              "lastName":      "Kumar",
              "email":         "manish.kumar.%s@transport.org",
              "phone":         "+919811100002",
              "licenseNumber": "DL-MANISH-%s"
            }
            """.formatted(
                UUID.randomUUID().toString().substring(0, 8),
                UUID.randomUUID().toString().substring(0, 8),
                UUID.randomUUID().toString().substring(0, 6)
            );

        backupDriverId = transport().when()
            .body(backupDriverBody).post("/api/v1/transport/drivers")
            .then().statusCode(201)
            .extract().jsonPath().getString("driverId");

        // 1d — Backup vehicle
        String backupPlate = "AS-01-ALT-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        String backupVehicleBody = """
            {
              "driverId":    "%s",
              "plateNumber": "%s",
              "type":        "MEDIUM_TRUCK",
              "capacityKg":  4000.0,
              "capacityCbm": 25.0
            }
            """.formatted(backupDriverId, backupPlate);

        backupVehicleId = transport().when()
            .body(backupVehicleBody).post("/api/v1/transport/vehicles")
            .then().statusCode(201)
            .extract().jsonPath().getString("vehicleId");

        log.info("Fleet registered: Primary Driver={}, Vehicle={}; Backup Driver={}, Vehicle={}",
            primaryDriverId, primaryVehicleId, backupDriverId, backupVehicleId);
    }

    @Test
    @Order(2)
    @DisplayName("Step 2: Create shipment and assign to Primary Driver and Vehicle 1")
    void step2_createAndAssignShipment() {
        String shipmentBody = """
            {
              "allocationId":      "%s",
              "reservationId":     "%s",
              "receiverId":        "%s",
              "scheduledPickupAt": "%s",
              "pickupAddress": {
                "street":  "Depot Road",
                "city":    "Guwahati",
                "state":   "Assam",
                "country": "IN",
                "zip":     "781001"
              },
              "deliveryAddress": {
                "street":  "Camp Alpha",
                "city":    "Jorhat",
                "state":   "Assam",
                "country": "IN",
                "zip":     "785001"
              }
            }
            """.formatted(ALLOCATION_ID, RESERVATION_ID, RECEIVER_ID,
                Instant.now().plus(2, ChronoUnit.HOURS));

        shipmentId = transport().when()
            .body(shipmentBody).post("/api/v1/transport/shipments")
            .then().statusCode(201)
            .extract().jsonPath().getString("shipmentId");

        assertThat(shipmentId).isNotNull();

        // Assign to primary driver
        String assignBody = """
            {
              "driverId":  "%s",
              "vehicleId": "%s"
            }
            """.formatted(primaryDriverId, primaryVehicleId);

        transport().when()
            .body(assignBody)
            .patch("/api/v1/transport/shipments/" + shipmentId + "/assign")
            .then().statusCode(200);

        // Verify primary driver is now busy and backup driver is available
        await().atMost(20, SECONDS).untilAsserted(() -> {
            boolean primaryAvail = dbQuery("transport",
                "SELECT is_available FROM drivers WHERE driver_id = ?::uuid", primaryDriverId)
                .equals("t") || dbQuery("transport",
                "SELECT is_available FROM drivers WHERE driver_id = ?::uuid", primaryDriverId)
                .equals("true");
            assertThat(primaryAvail).isFalse();

            boolean backupAvail = dbQuery("transport",
                "SELECT is_available FROM drivers WHERE driver_id = ?::uuid", backupDriverId)
                .equals("t") || dbQuery("transport",
                "SELECT is_available FROM drivers WHERE driver_id = ?::uuid", backupDriverId)
                .equals("true");
            assertThat(backupAvail).isTrue();
        });

        log.info("Shipment {} assigned to primary driver {}", shipmentId, primaryDriverId);
    }

    @Test
    @Order(3)
    @DisplayName("Step 3: Report breakdown for Vehicle 1 and verify automated reassignment")
    void step3_reportBreakdownAndVerifyReassignment() {
        String breakdownBody = """
            {
              "reason": "Transmission gear failure on NH37"
            }
            """;

        log.info("Reporting breakdown for vehicle: {}", primaryVehicleId);
        transport().when()
            .body(breakdownBody)
            .post("/api/v1/transport/vehicles/" + primaryVehicleId + "/breakdown")
            .then().statusCode(200);

        // 3a — Assert Vehicle 1 is deactivated
        await().atMost(30, SECONDS).untilAsserted(() -> {
            String activeStr = dbQuery("transport",
                "SELECT is_active FROM vehicles WHERE vehicle_id = ?::uuid", primaryVehicleId);
            assertThat(activeStr).isIn("f", "false");
        });

        // 3b — Assert Primary Driver is freed
        await().atMost(30, SECONDS).untilAsserted(() -> {
            String availStr = dbQuery("transport",
                "SELECT is_available FROM drivers WHERE driver_id = ?::uuid", primaryDriverId);
            assertThat(availStr).isIn("t", "true");
        });

        // 3c — Assert Shipment is reassigned to Backup Driver and Vehicle 2
        await().atMost(30, SECONDS).untilAsserted(() -> {
            String assignedDriver = dbQuery("transport",
                "SELECT driver_id::text FROM shipments WHERE shipment_id = ?::uuid", shipmentId);
            String assignedVehicle = dbQuery("transport",
                "SELECT vehicle_id::text FROM shipments WHERE shipment_id = ?::uuid", shipmentId);
            String status = dbQuery("transport",
                "SELECT status FROM shipments WHERE shipment_id = ?::uuid", shipmentId);

            assertThat(assignedDriver).isEqualTo(backupDriverId);
            assertThat(assignedVehicle).isEqualTo(backupVehicleId);
            assertThat(status).isEqualTo("ASSIGNED");
        });

        // 3d — Assert Backup Driver is now busy
        await().atMost(30, SECONDS).untilAsserted(() -> {
            String backupAvail = dbQuery("transport",
                "SELECT is_available FROM drivers WHERE driver_id = ?::uuid", backupDriverId);
            assertThat(backupAvail).isIn("f", "false");
        });

        // 3e — Assert REASSIGNED tracking event exists
        List<String> eventTypes = transport().when()
            .get("/api/v1/transport/shipments/" + shipmentId + "/tracking")
            .then().statusCode(200)
            .extract().jsonPath().getList("events.eventType", String.class);
        assertThat(eventTypes).contains("REASSIGNED");

        // 3f — Assert Kafka event re-emitted for new assignment
        KafkaEventCaptor.assertEventPublished(KAFKA, KafkaTopics.SHIPMENT_ASSIGNED,
            "ShipmentAssigned", 30);

        log.info("Vehicle breakdown handled cleanly: Vehicle 1 deactivated, shipment {} reassigned to Driver 2 ({})",
            shipmentId, backupDriverId);
    }
}
