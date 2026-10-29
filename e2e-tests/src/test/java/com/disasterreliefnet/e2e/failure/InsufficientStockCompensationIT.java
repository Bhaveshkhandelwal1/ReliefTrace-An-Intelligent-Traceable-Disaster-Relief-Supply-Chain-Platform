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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Failure Scenario 1: Insufficient Stock Compensation Saga Test.
 *
 * <p>Flow:
 * <ol>
 *   <li>Create a disaster and warehouse with only 50 kg of stock.</li>
 *   <li>Receiver submits a critical aid request for 300 kg.</li>
 *   <li>Verification case is approved.</li>
 *   <li>Coordinator creates and approves matching allocation for 300 kg.</li>
 *   <li>Inventory detects insufficient stock (300 > 50) and emits StockReservationFailed.</li>
 *   <li>Matching service processes failure compensation: allocation becomes RESERVATION_FAILED,
 *       matched request reverts to PENDING.</li>
 *   <li>Transport service creates no shipment for the failed allocation.</li>
 * </ol>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("E2E Failure Scenario 1: Insufficient Stock Saga Compensation")
public class InsufficientStockCompensationIT extends BaseE2ETest {

    private static final Logger log = LoggerFactory.getLogger(InsufficientStockCompensationIT.class);

    private static String disasterId;
    private static String warehouseId;
    private static String itemId;
    private static String receiverId;
    private static String requestId;
    private static String allocationId;

    @Test
    @Order(1)
    @DisplayName("Step 1: Declare disaster and inventory item with only 50 kg available")
    void step1_setupDisasterAndLowStock() {
        String disasterBody = """
            {
              "name":        "Test Low Stock Cyclone %s",
              "type":        "CYCLONE",
              "severity":    "LEVEL_4",
              "description": "Cyclone affecting coastal relief camps",
              "location": {
                "street":  "Coastal Road",
                "city":    "Puri",
                "state":   "Odisha",
                "country": "IN",
                "zip":     "752001"
              },
              "declaredAt": "%s"
            }
            """.formatted(UUID.randomUUID().toString().substring(0, 8), Instant.now().minus(1, ChronoUnit.DAYS));

        disasterId = disaster().when()
            .body(disasterBody).post("/api/v1/disasters")
            .then().statusCode(201)
            .extract().jsonPath().getString("disasterId");

        assertThat(disasterId).isNotNull();

        String warehouseBody = """
            {
              "name":     "Puri Relief Depot %s",
              "address": {
                "street":  "Depot Road",
                "city":    "Puri",
                "state":   "Odisha",
                "country": "IN",
                "zip":     "752001"
              },
              "capacity": 1000.0
            }
            """.formatted(UUID.randomUUID().toString().substring(0, 8));

        warehouseId = inventory().when()
            .body(warehouseBody).post("/api/v1/inventory/warehouses")
            .then().statusCode(201)
            .extract().jsonPath().getString("warehouseId");

        assertThat(warehouseId).isNotNull();

        // Create item with only 50 kg available stock
        String itemBody = """
            {
              "warehouseId":    "%s",
              "name":           "Limited Food Packets",
              "category":       "FOOD",
              "unit":           "kg",
              "quantityOnHand": 50.0
            }
            """.formatted(warehouseId);

        itemId = inventory().when()
            .body(itemBody).post("/api/v1/inventory/items")
            .then().statusCode(201)
            .extract().jsonPath().getString("itemId");

        assertThat(itemId).isNotNull();
        log.info("Disaster {}, Warehouse {}, Item {} (50 kg) created", disasterId, warehouseId, itemId);
    }

    @Test
    @Order(2)
    @DisplayName("Step 2: Receiver submits request for 300 kg and verification approves it")
    void step2_receiverSubmitAndVerify() {
        String receiverBody = """
            {
              "firstName":       "Sita",
              "lastName":        "Patel",
              "email":           "sita.patel.%s@camp.org",
              "phone":           "+919888800001",
              "disasterId":      "%s",
              "currentLocation": {
                "street":  "Shelter Point B",
                "city":    "Puri",
                "state":   "Odisha",
                "country": "IN",
                "zip":     "752001"
              },
              "householdSize":    60,
              "vulnerabilityTags": ["PREGNANT_WOMEN"]
            }
            """.formatted(UUID.randomUUID().toString().substring(0, 8), disasterId);

        receiverId = receiver().when()
            .body(receiverBody).post("/api/v1/receivers")
            .then().statusCode(201)
            .extract().jsonPath().getString("receiverId");

        // Submit request for 300 kg (exceeding 50 kg stock)
        String requestBody = """
            {
              "disasterId":        "%s",
              "category":          "FOOD",
              "description":       "Food aid for shelter residents",
              "quantityRequested": 300.0,
              "unit":              "kg",
              "urgencyLevel":      "HIGH",
              "deliveryAddress": {
                "street":  "Shelter Point B",
                "city":    "Puri",
                "state":   "Odisha",
                "country": "IN",
                "zip":     "752001"
              }
            }
            """.formatted(disasterId);

        requestId = receiver().when()
            .body(requestBody).post("/api/v1/receivers/" + receiverId + "/requests")
            .then().statusCode(201)
            .extract().jsonPath().getString("requestId");

        log.info("Receiver {} submitted request {} for 300 kg", receiverId, requestId);

        // Verification case auto-opened; approve it
        await().atMost(30, SECONDS).untilAsserted(() -> {
            String caseId = dbQuery("verification",
                "SELECT case_id::text FROM verification_cases WHERE request_id = ?::uuid", requestId);
            assertThat(caseId).isNotNull();

            verification().when()
                .body("{\"reviewNotes\": \"Approved by field verifier.\"}")
                .patch("/api/v1/verification/cases/" + caseId + "/approve")
                .then().statusCode(200);
        });

        // Request status should become VERIFIED
        await().atMost(30, SECONDS).untilAsserted(() -> {
            String status = dbQuery("receiver",
                "SELECT status FROM aid_requests WHERE request_id = ?::uuid", requestId);
            assertThat(status).isEqualTo("VERIFIED");
        });
    }

    @Test
    @Order(3)
    @DisplayName("Step 3: Coordinator creates and approves allocation of 300 kg")
    void step3_matchingAllocationApproval() {
        String allocBody = """
            {
              "requestId": "%s",
              "itemId":    "%s",
              "quantity":  300.0,
              "notes":     "High urgency allocation exceeding stock"
            }
            """.formatted(requestId, itemId);

        allocationId = matching().when()
            .body(allocBody).post("/api/v1/matching/allocations")
            .then().statusCode(201)
            .extract().jsonPath().getString("allocationId");

        assertThat(allocationId).isNotNull();

        matching().when()
            .body("{\"approvedBy\": \"coord-lowstock\", \"notes\": \"Approved for delivery\"}")
            .patch("/api/v1/matching/allocations/" + allocationId + "/approve")
            .then().statusCode(200);

        log.info("Allocation {} approved for 300 kg", allocationId);

        KafkaEventCaptor.assertEventPublished(KAFKA, KafkaTopics.ALLOCATION_APPROVED,
            "AllocationApproved", 30);
    }

    @Test
    @Order(4)
    @DisplayName("Step 4: Inventory fails reservation and emits StockReservationFailed")
    void step4_inventoryFailsReservation() {
        // Inventory receives AllocationApproved, checks stock (50 < 300), and emits StockReservationFailed
        KafkaEventCaptor.assertEventPublished(KAFKA, KafkaTopics.STOCK_RESERVATION_FAILED,
            "StockReservationFailed", 45);

        log.info("StockReservationFailed event detected on Kafka");
    }

    @Test
    @Order(5)
    @DisplayName("Step 5: Verify saga compensation in Matching and Transport services")
    void step5_verifySagaCompensation() {
        // Matching service: allocation status updated to RESERVATION_FAILED
        await().atMost(30, SECONDS).untilAsserted(() -> {
            String status = dbQuery("matching",
                "SELECT status FROM allocations WHERE allocation_id = ?::uuid", allocationId);
            assertThat(status).isEqualTo("RESERVATION_FAILED");
        });

        // Matching service: matched request reverted to PENDING
        await().atMost(30, SECONDS).untilAsserted(() -> {
            String reqStatus = dbQuery("matching",
                "SELECT status FROM matched_requests WHERE request_id = ?::uuid", requestId);
            assertThat(reqStatus).isEqualTo("PENDING");
        });

        // Transport service: NO shipment was created for this failed allocation
        int shipmentCount = dbCount("transport",
            "SELECT COUNT(*) FROM shipments WHERE allocation_id = ?::uuid", allocationId);
        assertThat(shipmentCount).isEqualTo(0);

        log.info("Saga compensation verified successfully. Allocation is RESERVATION_FAILED, request is PENDING, 0 shipments.");
    }
}
