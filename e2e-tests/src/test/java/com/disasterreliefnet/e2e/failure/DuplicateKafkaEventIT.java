package com.disasterreliefnet.e2e.failure;

import com.disasterreliefnet.common.event.KafkaTopics;
import com.disasterreliefnet.e2e.infrastructure.BaseE2ETest;
import com.disasterreliefnet.e2e.infrastructure.KafkaEventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Failure Scenario 2: Duplicate Kafka Event Idempotency Test.
 *
 * <p>Verifies that when duplicate Kafka events are delivered (at-least-once messaging),
 * the receiving service (Transport Service) processes the business logic exactly once
 * using the {@code processed_events} idempotency table guard.
 *
 * <p>Flow:
 * <ol>
 *   <li>Construct a {@code StockReserved} event envelope with a unique {@code eventId}.</li>
 *   <li>Publish to {@code stock.reserved} topic.</li>
 *   <li>Verify shipment created in Transport Service and recorded in {@code processed_events}.</li>
 *   <li>Publish the identical event envelope with the SAME {@code eventId} a second time.</li>
 *   <li>Assert only 1 shipment exists for the allocation and 1 record in {@code processed_events}.</li>
 * </ol>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("E2E Failure Scenario 2: Duplicate Kafka Event Idempotency")
public class DuplicateKafkaEventIT extends BaseE2ETest {

    private static final Logger log = LoggerFactory.getLogger(DuplicateKafkaEventIT.class);

    private static final UUID EVENT_ID       = UUID.randomUUID();
    private static final UUID ALLOCATION_ID  = UUID.randomUUID();
    private static final UUID RESERVATION_ID = UUID.randomUUID();
    private static final UUID REQUEST_ID     = UUID.randomUUID();
    private static final UUID RECEIVER_ID    = UUID.randomUUID();
    private static final UUID ITEM_ID        = UUID.randomUUID();
    private static final UUID WAREHOUSE_ID   = UUID.randomUUID();

    private static String envelopeJson;

    @Test
    @Order(1)
    @DisplayName("Step 1: Publish StockReserved event first time")
    void step1_publishFirstTime() {
        String payloadJson = """
            {
              "reservationId": "%s",
              "allocationId":  "%s",
              "requestId":      "%s",
              "receiverId":     "%s",
              "itemId":         "%s",
              "warehouseId":    "%s",
              "quantity":       250.0,
              "unit":           "kg",
              "pickupAddress": {
                "street":  "Depot 44",
                "city":    "Guwahati",
                "state":   "Assam",
                "country": "IN",
                "zip":     "781001"
              },
              "deliveryAddress": {
                "street":  "Camp Gamma",
                "city":    "Nagaon",
                "state":   "Assam",
                "country": "IN",
                "zip":     "782001"
              }
            }
            """.formatted(RESERVATION_ID, ALLOCATION_ID, REQUEST_ID, RECEIVER_ID, ITEM_ID, WAREHOUSE_ID);

        envelopeJson = KafkaEventPublisher.buildEnvelope(
            EVENT_ID, "StockReserved", "RESERVATION", RESERVATION_ID.toString(), payloadJson);

        log.info("Publishing initial StockReserved event with eventId: {}", EVENT_ID);
        KafkaEventPublisher.publish(KAFKA, KafkaTopics.STOCK_RESERVED, ALLOCATION_ID.toString(), envelopeJson);

        // Await shipment creation in Transport DB
        await().atMost(30, SECONDS).untilAsserted(() -> {
            int shipmentCount = dbCount("transport",
                "SELECT COUNT(*) FROM shipments WHERE allocation_id = ?::uuid", ALLOCATION_ID.toString());
            assertThat(shipmentCount).isEqualTo(1);
        });

        // Await idempotency record in processed_events table
        await().atMost(30, SECONDS).untilAsserted(() -> {
            int processedCount = dbCount("transport",
                "SELECT COUNT(*) FROM processed_events WHERE event_id = ?::uuid", EVENT_ID.toString());
            assertThat(processedCount).isEqualTo(1);
        });

        log.info("First event processed. Shipment created and processed_events entry recorded.");
    }

    @Test
    @Order(2)
    @DisplayName("Step 2: Publish identical duplicate StockReserved event and assert idempotency")
    void step2_publishDuplicateAndAssertIdempotency() {
        log.info("Publishing duplicate StockReserved event with identical eventId: {}", EVENT_ID);

        // Publish the exact same event envelope again
        KafkaEventPublisher.publish(KAFKA, KafkaTopics.STOCK_RESERVED, ALLOCATION_ID.toString(), envelopeJson);

        // Wait a reasonable observation window to allow consumer to receive and process
        try {
            Thread.sleep(3000);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }

        // Verify that still exactly ONE shipment exists for this allocationId
        int shipmentCount = dbCount("transport",
            "SELECT COUNT(*) FROM shipments WHERE allocation_id = ?::uuid", ALLOCATION_ID.toString());
        assertThat(shipmentCount)
            .as("Duplicate event must not create duplicate shipments")
            .isEqualTo(1);

        // Verify that processed_events has exactly ONE record for this eventId
        int processedCount = dbCount("transport",
            "SELECT COUNT(*) FROM processed_events WHERE event_id = ?::uuid", EVENT_ID.toString());
        assertThat(processedCount)
            .as("processed_events table must only record the event once")
            .isEqualTo(1);

        log.info("Duplicate event successfully filtered by idempotency check. Test passed.");
    }
}
