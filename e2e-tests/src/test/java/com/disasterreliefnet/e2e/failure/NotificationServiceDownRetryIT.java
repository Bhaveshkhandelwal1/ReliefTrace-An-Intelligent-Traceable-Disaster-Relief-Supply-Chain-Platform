package com.disasterreliefnet.e2e.failure;

import com.disasterreliefnet.common.event.KafkaTopics;
import com.disasterreliefnet.e2e.infrastructure.BaseE2ETest;
import com.disasterreliefnet.e2e.infrastructure.KafkaEventCaptor;
import com.disasterreliefnet.e2e.infrastructure.KafkaEventPublisher;
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
 * Failure Scenario 4: Downstream Consumer Down and Event Replay Resilience Test.
 *
 * <p>Verifies the Transactional Outbox Pattern and Kafka decoupling guarantees:
 * <ol>
 *   <li>The upstream service (Donor Service) completes business transactions and publishes
 *       events to Kafka even if the downstream consumer (Notification Service) is stopped.</li>
 *   <li>Events persist reliably on Kafka topics without data loss.</li>
 *   <li>When the consumer is restarted, it consumes from committed/earliest offsets and
 *       successfully writes to the {@code notification_logs} and {@code processed_events} tables.</li>
 * </ol>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("E2E Failure Scenario 4: Notification Service Down and Replay Resilience")
public class NotificationServiceDownRetryIT extends BaseE2ETest {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceDownRetryIT.class);

    private static String disasterId;
    private static String donorId;
    private static String donationId;

    private static final UUID DIRECT_EVENT_ID = UUID.randomUUID();
    private static final UUID DIRECT_DONOR_ID = UUID.randomUUID();

    @Test
    @Order(1)
    @DisplayName("Step 1: Setup disaster and donor")
    void step1_setupDisasterAndDonor() {
        String disasterBody = """
            {
              "name":        "Test Earthquake Resilience %s",
              "type":        "EARTHQUAKE",
              "severity":    "LEVEL_3",
              "description": "Test disaster for consumer downtime scenario",
              "location": {
                "street":  "Hill Road",
                "city":    "Shillong",
                "state":   "Meghalaya",
                "country": "IN",
                "zip":     "793001"
              },
              "declaredAt": "%s"
            }
            """.formatted(UUID.randomUUID().toString().substring(0, 8), Instant.now().minus(2, ChronoUnit.DAYS));

        disasterId = disaster().when()
            .body(disasterBody).post("/api/v1/disasters")
            .then().statusCode(201)
            .extract().jsonPath().getString("disasterId");

        String donorBody = """
            {
              "firstName": "Vikram",
              "lastName":  "Singhania",
              "email":     "vikram.%s@philanthropy.org",
              "phone":     "+919877700001",
              "donorType": "CORPORATE",
              "address": {
                "street":  "7 Corporate Way",
                "city":    "Kolkata",
                "state":   "West Bengal",
                "country": "IN",
                "zip":     "700001"
              }
            }
            """.formatted(UUID.randomUUID().toString().substring(0, 8));

        donorId = donor().when()
            .body(donorBody).post("/api/v1/donors")
            .then().statusCode(201)
            .extract().jsonPath().getString("donorId");

        assertThat(disasterId).isNotNull();
        assertThat(donorId).isNotNull();
        log.info("Disaster {} and Donor {} created", disasterId, donorId);
    }

    @Test
    @Order(2)
    @DisplayName("Step 2: Stop Notification Service container and verify upstream donor operation succeeds")
    void step2_stopNotificationServiceAndProduceEvents() {
        log.info("Stopping notification-service container to simulate consumer outage...");
        notificationService.stop();
        assertThat(notificationService.isRunning()).isFalse();

        // 2a — Register donation via Donor Service REST API while notification service is DOWN
        String donationBody = """
            {
              "donationType":   "IN_KIND",
              "itemDescription":"Blankets and Warm Clothes",
              "quantityKg":     500,
              "disasterId":     "%s",
              "notes":          "Donation made while notification service is unreachable"
            }
            """.formatted(disasterId);

        donationId = donor().when()
            .body(donationBody)
            .post("/api/v1/donors/" + donorId + "/donations")
            .then().statusCode(201)
            .extract().jsonPath().getString("donationId");

        assertThat(donationId).isNotNull();
        log.info("Donation {} created successfully despite notification service being offline", donationId);

        // 2b — Assert outbox record in donor DB is marked published
        await().atMost(30, SECONDS).untilAsserted(() -> {
            boolean published = dbQuery("donor",
                "SELECT published FROM outbox_events WHERE aggregate_id = ?::uuid", donationId)
                .equals("t") || dbQuery("donor",
                "SELECT published FROM outbox_events WHERE aggregate_id = ?::uuid", donationId)
                .equals("true");
            assertThat(published).isTrue();
        });

        // 2c — Assert Kafka contains the DonationRegistered event
        KafkaEventCaptor.assertEventPublished(KAFKA, KafkaTopics.DONATION_REGISTERED,
            "DonationRegistered", 30);

        // 2d — Also publish a second event directly to Kafka to test backlog replay
        String directPayload = """
            {
              "donationId":   "%s",
              "donorId":      "%s",
              "donorName":    "Direct Backlog Donor",
              "donationType": "IN_KIND",
              "amount":       null,
              "disasterId":   "%s",
              "registeredAt": "%s"
            }
            """.formatted(UUID.randomUUID(), DIRECT_DONOR_ID, disasterId, Instant.now().toString());

        String envelope = KafkaEventPublisher.buildEnvelope(
            DIRECT_EVENT_ID, "DonationRegistered", "DONATION", DIRECT_DONOR_ID.toString(), directPayload);

        KafkaEventPublisher.publish(KAFKA, KafkaTopics.DONATION_REGISTERED, DIRECT_DONOR_ID.toString(), envelope);
        log.info("Direct backlog event published to Kafka. Notification consumer still offline.");
    }

    @Test
    @Order(3)
    @DisplayName("Step 3: Restart Notification Service and assert all pending notifications are consumed")
    void step3_restartConsumerAndVerifyZeroDataLoss() {
        log.info("Restarting notification-service container...");
        notificationService.start();
        assertThat(notificationService.isRunning()).isTrue();

        // 3a — Await notification_logs entry for the donor service API event
        await().atMost(45, SECONDS).untilAsserted(() -> {
            int logCount = dbCount("notification",
                "SELECT COUNT(*) FROM notification_logs WHERE user_id = ?", donorId);
            assertThat(logCount)
                .as("Notification service must process backlog event from donor API")
                .isGreaterThanOrEqualTo(1);
        });

        // 3b — Await notification_logs entry for the direct backlog event
        await().atMost(45, SECONDS).untilAsserted(() -> {
            int directLogCount = dbCount("notification",
                "SELECT COUNT(*) FROM notification_logs WHERE event_id = ?::uuid", DIRECT_EVENT_ID.toString());
            assertThat(directLogCount)
                .as("Notification service must process direct backlog event upon resumption")
                .isEqualTo(1);
        });

        // 3c — Assert processed_events has recorded the processed events
        await().atMost(30, SECONDS).untilAsserted(() -> {
            int processedCount = dbCount("notification",
                "SELECT COUNT(*) FROM processed_events WHERE event_id = ?::uuid", DIRECT_EVENT_ID.toString());
            assertThat(processedCount).isEqualTo(1);
        });

        log.info("Notification service downtime recovery verified: 100% of backlog processed with zero data loss.");
    }
}
