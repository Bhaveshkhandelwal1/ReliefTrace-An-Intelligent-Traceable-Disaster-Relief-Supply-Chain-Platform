package com.disasterreliefnet.e2e.saga;

import com.disasterreliefnet.common.event.KafkaTopics;
import com.disasterreliefnet.e2e.infrastructure.BaseE2ETest;
import com.disasterreliefnet.e2e.infrastructure.KafkaEventCaptor;
import io.restassured.response.ValidatableResponse;
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

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * End-to-end saga integration test covering the complete happy path:
 *
 * <ol>
 *   <li>Declare a disaster</li>
 *   <li>Donor registers a donation of 1 000 kg of food packets (IN_KIND)</li>
 *   <li>Warehouse is created and stocked with 1 000 kg</li>
 *   <li>Two receivers (camps) register and submit CRITICAL/HIGH aid requests</li>
 *   <li>Verification service approves both cases</li>
 *   <li>Matching suggests allocations; coordinator approves the CRITICAL one first</li>
 *   <li>Inventory reserves stock → StockReserved event</li>
 *   <li>Transport service creates shipment → coordinator assigns driver and vehicle</li>
 *   <li>Driver picks up goods → IN_TRANSIT</li>
 *   <li>Driver delivers goods → DELIVERED</li>
 *   <li>Final state is asserted across every service DB and via REST</li>
 * </ol>
 *
 * <p>All services run as real Docker containers via Testcontainers. No mocks.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Full Saga: Donation → Allocation → Delivery")
class FullSagaIT extends BaseE2ETest {

    private static final Logger log = LoggerFactory.getLogger(FullSagaIT.class);

    // IDs accumulated as the saga progresses (shared across ordered test methods)
    private static String disasterId;
    private static String donorId;
    private static String donationId;
    private static String warehouseId;
    private static String itemId;
    private static String receiverIdCritical;
    private static String receiverIdHigh;
    private static String requestIdCritical;
    private static String requestIdHigh;
    private static String verificationCaseIdCritical;
    private static String allocationId;
    private static String reservationId;
    private static String shipmentId;
    private static String driverId;
    private static String vehicleId;

    // ──────────────────────────────────────────────────────────────────────────
    // Step 1 — Disaster declaration
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("Step 1: Coordinator declares a SEVERE FLOOD disaster")
    void step1_declareDisaster() {
        String body = """
            {
              "name":          "Assam Flood 2024",
              "type":          "FLOOD",
              "severityLevel": "SEVERE",
              "description":   "Major flooding in Assam affecting 50,000 people",
              "affectedArea":  {
                "latitude":   26.2,
                "longitude":  91.7,
                "radiusKm":   80.0,
                "regionName": "Assam Valley",
                "country":    "IN"
              },
              "occurredAt": "%s"
            }
            """.formatted(Instant.now().minus(2, ChronoUnit.HOURS));

        disasterId = disaster().when()
            .body(body).post("/api/v1/disasters")
            .then().statusCode(201)
            .extract().jsonPath().getString("disasterId");

        log.info("Disaster declared: {}", disasterId);
        assertThat(disasterId).isNotNull();

        // Verify DisasterDeclared event on Kafka
        KafkaEventCaptor.assertEventPublished(KAFKA, KafkaTopics.DISASTER_DECLARED,
            "DisasterDeclared", 30);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Step 2 — Donor registers a donation of 1000 food packets
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @Order(2)
    @DisplayName("Step 2: Donor registers and submits 1000 kg IN_KIND food donation")
    void step2_donorRegistersDonation() {
        // 2a — Register donor
        String donorBody = """
            {
              "firstName": "Anita",
              "lastName":  "Sharma",
              "email":     "anita.sharma@ngo.in",
              "phone":     "+919876543210",
              "address": {
                "street":  "12 Relief Road",
                "city":    "Mumbai",
                "state":   "Maharashtra",
                "country": "IN",
                "zip":     "400001"
              }
            }
            """;

        donorId = donor().when()
            .body(donorBody).post("/api/v1/donors")
            .then().statusCode(201)
            .extract().jsonPath().getString("donorId");

        log.info("Donor registered: {}", donorId);
        assertThat(donorId).isNotNull();

        // 2b — Submit IN_KIND donation of 1000 kg food
        String donationBody = """
            {
              "donationType":   "IN_KIND",
              "itemDescription":"Emergency food packets (rice + lentils)",
              "quantityKg":     1000,
              "disasterId":     "%s",
              "notes":          "Vacuum-packed, 6-month shelf life"
            }
            """.formatted(disasterId);

        donationId = donor().when()
            .body(donationBody)
            .post("/api/v1/donors/" + donorId + "/donations")
            .then().statusCode(201)
            .extract().jsonPath().getString("donationId");

        log.info("Donation registered: {}", donationId);
        assertThat(donationId).isNotNull();

        // Assert DonationRegistered on Kafka
        KafkaEventCaptor.assertEventPublished(KAFKA, KafkaTopics.DONATION_REGISTERED,
            "DonationRegistered", 30);

        // Assert donor DB state
        String donorStatus = dbQuery("donor",
            "SELECT status FROM donations WHERE donation_id = ?::uuid", donationId);
        assertThat(donorStatus).isEqualTo("PENDING");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Step 3 — Create warehouse and add 1000 kg stock
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("Step 3: Warehouse created and stocked with 1000 kg food")
    void step3_warehouseAndStock() {
        // 3a — Create warehouse
        String warehouseBody = """
            {
              "name":     "Guwahati Central Relief Hub",
              "address":  {
                "street":  "NH27 Bypass",
                "city":    "Guwahati",
                "state":   "Assam",
                "country": "IN",
                "zip":     "781001"
              },
              "capacity": 5000.0
            }
            """;

        warehouseId = inventory().when()
            .body(warehouseBody).post("/api/v1/inventory/warehouses")
            .then().statusCode(201)
            .extract().jsonPath().getString("warehouseId");

        log.info("Warehouse created: {}", warehouseId);
        assertThat(warehouseId).isNotNull();

        // 3b — Create item with 1000 kg stock
        String itemBody = """
            {
              "warehouseId":    "%s",
              "name":           "Emergency Food Packets",
              "category":       "FOOD",
              "unit":           "kg",
              "quantityOnHand": 1000.0
            }
            """.formatted(warehouseId);

        itemId = inventory().when()
            .body(itemBody).post("/api/v1/inventory/items")
            .then().statusCode(201)
            .extract().jsonPath().getString("itemId");

        log.info("Item created with 1000 kg: {}", itemId);
        assertThat(itemId).isNotNull();

        // Assert correct stock level in DB
        double qty = DB.get("inventory")
            .queryForObject("SELECT quantity_on_hand FROM items WHERE item_id = ?::uuid",
                Double.class, itemId);
        assertThat(qty).isEqualTo(1000.0);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Step 4 — Two verified camps submit requests with different urgency
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @Order(4)
    @DisplayName("Step 4: Two receiver camps submit aid requests (CRITICAL + HIGH)")
    void step4_receiversSubmitRequests() {
        // ── Receiver 1: CRITICAL urgency ──────────────────────────────────────
        String r1Body = """
            {
              "firstName":       "Rahim",
              "lastName":        "Ali",
              "email":           "rahim.ali@camp1.in",
              "phone":           "+919000000001",
              "disasterId":      "%s",
              "currentLocation": {
                "street":  "Camp Alpha",
                "city":    "Jorhat",
                "state":   "Assam",
                "country": "IN",
                "zip":     "785001"
              },
              "householdSize":    150,
              "vulnerabilityTags": ["ELDERLY", "CHILDREN"]
            }
            """.formatted(disasterId);

        receiverIdCritical = receiver().when()
            .body(r1Body).post("/api/v1/receivers")
            .then().statusCode(201)
            .extract().jsonPath().getString("receiverId");

        String req1Body = """
            {
              "disasterId":        "%s",
              "category":          "FOOD",
              "description":       "Urgent food for 150 flood victims — elderly and children",
              "quantityRequested": 300.0,
              "unit":              "kg",
              "urgencyLevel":      "CRITICAL",
              "deliveryAddress":   {
                "street":  "Camp Alpha Relief Point",
                "city":    "Jorhat",
                "state":   "Assam",
                "country": "IN",
                "zip":     "785001"
              }
            }
            """.formatted(disasterId);

        requestIdCritical = receiver().when()
            .body(req1Body)
            .post("/api/v1/receivers/" + receiverIdCritical + "/requests")
            .then().statusCode(201)
            .extract().jsonPath().getString("requestId");

        log.info("CRITICAL request: {}", requestIdCritical);

        // ── Receiver 2: HIGH urgency ──────────────────────────────────────────
        String r2Body = """
            {
              "firstName":       "Priya",
              "lastName":        "Das",
              "email":           "priya.das@camp2.in",
              "phone":           "+919000000002",
              "disasterId":      "%s",
              "currentLocation": {
                "street":  "Camp Beta",
                "city":    "Dibrugarh",
                "state":   "Assam",
                "country": "IN",
                "zip":     "786001"
              },
              "householdSize":    80,
              "vulnerabilityTags": ["DISABLED"]
            }
            """.formatted(disasterId);

        receiverIdHigh = receiver().when()
            .body(r2Body).post("/api/v1/receivers")
            .then().statusCode(201)
            .extract().jsonPath().getString("receiverId");

        String req2Body = """
            {
              "disasterId":        "%s",
              "category":          "FOOD",
              "description":       "Food for 80 flood-displaced disabled individuals",
              "quantityRequested": 150.0,
              "unit":              "kg",
              "urgencyLevel":      "HIGH",
              "deliveryAddress":   {
                "street":  "Camp Beta Relief Point",
                "city":    "Dibrugarh",
                "state":   "Assam",
                "country": "IN",
                "zip":     "786001"
              }
            }
            """.formatted(disasterId);

        requestIdHigh = receiver().when()
            .body(req2Body)
            .post("/api/v1/receivers/" + receiverIdHigh + "/requests")
            .then().statusCode(201)
            .extract().jsonPath().getString("requestId");

        log.info("HIGH request: {}", requestIdHigh);

        // Both RequestCreated events must arrive
        await().atMost(30, SECONDS).untilAsserted(() ->
            assertThat(KafkaEventCaptor.countEvents(KAFKA,
                KafkaTopics.REQUEST_CREATED, "RequestCreated", 5))
                .isGreaterThanOrEqualTo(2)
        );

        // Verification service should auto-open cases
        await().atMost(30, SECONDS).untilAsserted(() ->
            assertThat(dbCount("verification",
                "SELECT COUNT(*) FROM verification_cases WHERE request_id = ?::uuid",
                requestIdCritical))
                .isEqualTo(1)
        );
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Step 5 — Verification service approves the CRITICAL case
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @Order(5)
    @DisplayName("Step 5: Coordinator approves the CRITICAL verification case")
    void step5_verifyCriticalRequest() {
        // Find the case for the CRITICAL request
        await().atMost(20, SECONDS).untilAsserted(() -> {
            verificationCaseIdCritical = DB.get("verification")
                .queryForObject(
                    "SELECT case_id::text FROM verification_cases WHERE request_id = ?::uuid",
                    String.class, requestIdCritical);
            assertThat(verificationCaseIdCritical).isNotNull();
        });

        log.info("Approving verification case: {}", verificationCaseIdCritical);

        verification().when()
            .body("{\"reviewNotes\": \"Verified by field coordinator. Camp Alpha confirmed.\"}")
            .patch("/api/v1/verification/cases/" + verificationCaseIdCritical + "/approve")
            .then().statusCode(200);

        // Await RequestVerified event
        KafkaEventCaptor.assertEventPublished(KAFKA, KafkaTopics.REQUEST_VERIFIED,
            "RequestVerified", 30);

        // Receiver service should update request to VERIFIED
        await().atMost(30, SECONDS).untilAsserted(() -> {
            String status = dbQuery("receiver",
                "SELECT status FROM aid_requests WHERE request_id = ?::uuid",
                requestIdCritical);
            assertThat(status).isEqualTo("VERIFIED");
        });
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Step 6 — Matching: coordinator creates and approves allocation
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @Order(6)
    @DisplayName("Step 6: Matching service suggests allocation; coordinator approves CRITICAL")
    void step6_matchingApproval() {
        // 6a — Create allocation
        String allocationBody = """
            {
              "requestId": "%s",
              "itemId":    "%s",
              "quantity":  300.0,
              "notes":     "CRITICAL urgency — prioritised over HIGH request"
            }
            """.formatted(requestIdCritical, itemId);

        allocationId = matching().when()
            .body(allocationBody).post("/api/v1/matching/allocations")
            .then().statusCode(201)
            .extract().jsonPath().getString("allocationId");

        log.info("Allocation created: {}", allocationId);
        assertThat(allocationId).isNotNull();

        // 6b — Coordinator approves the allocation
        matching().when()
            .body("{\"approvedBy\": \"coordinator-01\", \"notes\": \"Approved for Camp Alpha\"}")
            .patch("/api/v1/matching/allocations/" + allocationId + "/approve")
            .then().statusCode(200);

        // AllocationApproved event on Kafka
        KafkaEventCaptor.assertEventPublished(KAFKA, KafkaTopics.ALLOCATION_APPROVED,
            "AllocationApproved", 30);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Step 7 — Inventory reserves 300 kg
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @Order(7)
    @DisplayName("Step 7: Inventory reserves 300 kg and emits StockReserved")
    void step7_inventoryReservation() {
        // Await async reservation (triggered by AllocationApproved event)
        await().atMost(45, SECONDS).untilAsserted(() -> {
            int reservationCount = dbCount("inventory",
                "SELECT COUNT(*) FROM reservations WHERE allocation_id = ?::uuid",
                allocationId);
            assertThat(reservationCount).isEqualTo(1);
        });

        reservationId = DB.get("inventory")
            .queryForObject(
                "SELECT reservation_id::text FROM reservations WHERE allocation_id = ?::uuid",
                String.class, allocationId);

        log.info("Reservation created: {}", reservationId);

        String reservationStatus = dbQuery("inventory",
            "SELECT status FROM reservations WHERE reservation_id = ?::uuid", reservationId);
        assertThat(reservationStatus).isIn("CONFIRMED", "PENDING");

        // StockReserved on Kafka
        KafkaEventCaptor.assertEventPublished(KAFKA, KafkaTopics.STOCK_RESERVED,
            "StockReserved", 30);

        // Assert available stock reduced
        await().atMost(20, SECONDS).untilAsserted(() -> {
            double reserved = DB.get("inventory")
                .queryForObject("SELECT quantity_reserved FROM items WHERE item_id = ?::uuid",
                    Double.class, itemId);
            assertThat(reserved).isEqualTo(300.0);
        });

        // Receiver request should be ALLOCATED
        await().atMost(30, SECONDS).untilAsserted(() -> {
            String status = dbQuery("receiver",
                "SELECT status FROM aid_requests WHERE request_id = ?::uuid",
                requestIdCritical);
            assertThat(status).isEqualTo("ALLOCATED");
        });
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Step 8 — Transport service creates shipment, coordinator assigns driver
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @Order(8)
    @DisplayName("Step 8: Transport creates shipment; coordinator assigns driver + vehicle")
    void step8_transportAssignment() {
        // 8a — Register driver
        String driverBody = """
            {
              "userId":        "driver-user-001",
              "firstName":     "Arjun",
              "lastName":      "Verma",
              "email":         "arjun.verma@transport.in",
              "phone":         "+919800000001",
              "licenseNumber": "AS-DL-2024-001"
            }
            """;

        driverId = transport().when()
            .body(driverBody).post("/api/v1/transport/drivers")
            .then().statusCode(201)
            .extract().jsonPath().getString("driverId");

        // 8b — Register vehicle for driver
        String vehicleBody = """
            {
              "driverId":    "%s",
              "plateNumber": "AS-01-9988",
              "type":        "TRUCK",
              "capacityKg":  2000.0,
              "capacityCbm": 20.0
            }
            """.formatted(driverId);

        vehicleId = transport().when()
            .body(vehicleBody).post("/api/v1/transport/vehicles")
            .then().statusCode(201)
            .extract().jsonPath().getString("vehicleId");

        log.info("Driver: {}, Vehicle: {}", driverId, vehicleId);

        // 8c — Await shipment creation (triggered by StockReserved event)
        await().atMost(45, SECONDS).untilAsserted(() -> {
            int shipmentCount = dbCount("transport",
                "SELECT COUNT(*) FROM shipments WHERE allocation_id = ?::uuid",
                allocationId);
            assertThat(shipmentCount).isEqualTo(1);
        });

        shipmentId = DB.get("transport")
            .queryForObject(
                "SELECT shipment_id::text FROM shipments WHERE allocation_id = ?::uuid",
                String.class, allocationId);

        log.info("Shipment created: {}", shipmentId);

        // 8d — Coordinator assigns driver + vehicle
        String assignBody = """
            {
              "driverId":  "%s",
              "vehicleId": "%s"
            }
            """.formatted(driverId, vehicleId);

        transport().when()
            .body(assignBody)
            .patch("/api/v1/transport/shipments/" + shipmentId + "/assign")
            .then().statusCode(200);

        // ShipmentAssigned event on Kafka
        KafkaEventCaptor.assertEventPublished(KAFKA, KafkaTopics.SHIPMENT_ASSIGNED,
            "ShipmentAssigned", 30);

        String shipmentStatus = dbQuery("transport",
            "SELECT status FROM shipments WHERE shipment_id = ?::uuid", shipmentId);
        assertThat(shipmentStatus).isEqualTo("ASSIGNED");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Step 9 — Driver picks up goods
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @Order(9)
    @DisplayName("Step 9: Driver marks shipment as picked up → PICKED_UP")
    void step9_driverPicksUp() {
        transport().when()
            .patch("/api/v1/transport/shipments/" + shipmentId + "/pickup")
            .then().statusCode(200);

        KafkaEventCaptor.assertEventPublished(KAFKA, KafkaTopics.SHIPMENT_PICKED_UP,
            "ShipmentPickedUp", 20);

        String status = dbQuery("transport",
            "SELECT status FROM shipments WHERE shipment_id = ?::uuid", shipmentId);
        assertThat(status).isEqualTo("PICKED_UP");

        log.info("Shipment {} is PICKED_UP", shipmentId);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Step 10 — Driver delivers goods
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @Order(10)
    @DisplayName("Step 10: Driver delivers goods → DELIVERED; receiver confirms")
    void step10_deliverAndConfirm() {
        String deliverBody = """
            {
              "proofOfDeliveryUrl": "https://cdn.disasterreliefnet.in/pod/%s.jpg",
              "recipientSignature": "Rahim Ali",
              "deliveredAt":        "%s"
            }
            """.formatted(shipmentId, Instant.now());

        transport().when()
            .body(deliverBody)
            .patch("/api/v1/transport/shipments/" + shipmentId + "/deliver")
            .then().statusCode(200);

        // ShipmentDelivered on Kafka
        KafkaEventCaptor.assertEventPublished(KAFKA, KafkaTopics.SHIPMENT_DELIVERED,
            "ShipmentDelivered", 30);

        // ── Assert final state across all services ─────────────────────────────

        // Transport: shipment DELIVERED
        assertThat(dbQuery("transport",
            "SELECT status FROM shipments WHERE shipment_id = ?::uuid", shipmentId))
            .isEqualTo("DELIVERED");

        // Transport: driver freed
        await().atMost(20, SECONDS).untilAsserted(() ->
            assertThat(dbQuery("transport",
                "SELECT available::text FROM drivers WHERE driver_id = ?::uuid", driverId))
                .isEqualTo("true")
        );

        // Inventory: reservation CONSUMED
        await().atMost(30, SECONDS).untilAsserted(() ->
            assertThat(dbQuery("inventory",
                "SELECT status FROM reservations WHERE reservation_id = ?::uuid", reservationId))
                .isEqualTo("CONSUMED")
        );

        // Receiver: request DELIVERED
        await().atMost(30, SECONDS).untilAsserted(() ->
            assertThat(dbQuery("receiver",
                "SELECT status FROM aid_requests WHERE request_id = ?::uuid", requestIdCritical))
                .isEqualTo("DELIVERED")
        );

        // Transport: tracking events contain DELIVERED entry
        int trackingEvents = dbCount("transport",
            "SELECT COUNT(*) FROM shipment_tracking_events WHERE shipment_id = ?::uuid AND event_type = 'DELIVERED'",
            shipmentId);
        assertThat(trackingEvents).isGreaterThanOrEqualTo(1);

        log.info("Full saga completed successfully. Shipment {} DELIVERED.", shipmentId);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Step 11 — Final REST assertions across all services
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @Order(11)
    @DisplayName("Step 11: REST state verification across all services")
    void step11_finalRestAssertions() {
        // Shipment tracking via REST
        ValidatableResponse tracking = transport().when()
            .get("/api/v1/transport/shipments/" + shipmentId + "/tracking")
            .then().statusCode(200);

        tracking
            .extract().jsonPath().getString("status")
            .equals("DELIVERED");

        List<String> eventTypes = tracking.extract().jsonPath()
            .getList("events.eventType", String.class);
        assertThat(eventTypes).contains("CREATED", "ASSIGNED", "PICKED_UP", "DELIVERED");

        // Reservation via REST
        inventory().when()
            .get("/api/v1/inventory/reservations/" + reservationId)
            .then().statusCode(200);

        // Matching allocation via REST
        matching().when()
            .get("/api/v1/matching/allocations/" + allocationId)
            .then().statusCode(200);

        // Donor donation still exists
        donor().when()
            .get("/api/v1/donors/" + donorId + "/donations/" + donationId)
            .then().statusCode(200);

        log.info("All REST assertions passed.");
    }
}
