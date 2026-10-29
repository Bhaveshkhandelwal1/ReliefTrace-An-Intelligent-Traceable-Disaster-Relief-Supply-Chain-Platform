package com.disasterreliefnet.e2e.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Base class for all end-to-end integration tests.
 *
 * <p>Starts the entire DisasterReliefNet microservice stack inside Docker via
 * Testcontainers using a shared network. Each domain service gets its own
 * PostgreSQL database container. A single Kafka broker and Redis instance are
 * shared across all services.
 *
 * <p>All containers are started once per test suite run (JUnit 5 lifecycle
 * {@code PER_CLASS} on this base class). Subclasses inherit started containers
 * and pre-built REST-Assured {@code RequestSpecification} objects.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseE2ETest {

    protected static final Logger log = LoggerFactory.getLogger(BaseE2ETest.class);

    // ─────────────────────────── Docker image names ───────────────────────────

    private static final String POSTGRES_IMAGE = "postgres:16-alpine";
    private static final String KAFKA_IMAGE    = "confluentinc/cp-kafka:7.7.0";
    private static final String REDIS_IMAGE    = "redis:7.4-alpine";

    // ─────────────────────────── Service image names ──────────────────────────
    // These are built from the project Dockerfiles.  Run `mvn package` first.

    private static final String SVC_IMAGE_PREFIX = "disasterreliefnet";

    // ─────────────────────────── Service ports (internal) ─────────────────────

    protected static final int DONOR_PORT        = 8081;
    protected static final int RECEIVER_PORT     = 8082;
    protected static final int INVENTORY_PORT    = 8083;
    protected static final int MATCHING_PORT     = 8084;
    protected static final int TRANSPORT_PORT    = 8085;
    protected static final int VERIFICATION_PORT = 8086;
    protected static final int DISASTER_PORT     = 8087;
    protected static final int NOTIFICATION_PORT = 8088;
    protected static final int FINANCE_PORT      = 8089;

    // ─────────────────────────── Shared infrastructure ────────────────────────

    protected static final Network NETWORK = Network.newNetwork();

    protected static final KafkaContainer KAFKA =
        new KafkaContainer(DockerImageName.parse(KAFKA_IMAGE))
            .withNetwork(NETWORK)
            .withNetworkAliases("kafka")
            .withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "true")
            .withStartupTimeout(Duration.ofSeconds(90));

    @SuppressWarnings("resource")
    protected static final GenericContainer<?> REDIS =
        new GenericContainer<>(DockerImageName.parse(REDIS_IMAGE))
            .withNetwork(NETWORK)
            .withNetworkAliases("redis")
            .withExposedPorts(6379)
            .withStartupTimeout(Duration.ofSeconds(30));

    // ─────────────────────── Per-service PostgreSQL databases ─────────────────

    protected static final PostgreSQLContainer<?> DONOR_DB =
        serviceDb("donor_db");
    protected static final PostgreSQLContainer<?> RECEIVER_DB =
        serviceDb("receiver_db");
    protected static final PostgreSQLContainer<?> INVENTORY_DB =
        serviceDb("inventory_db");
    protected static final PostgreSQLContainer<?> MATCHING_DB =
        serviceDb("matching_db");
    protected static final PostgreSQLContainer<?> TRANSPORT_DB =
        serviceDb("transport_db");
    protected static final PostgreSQLContainer<?> VERIFICATION_DB =
        serviceDb("verification_db");
    protected static final PostgreSQLContainer<?> DISASTER_DB =
        serviceDb("disaster_db");
    protected static final PostgreSQLContainer<?> FINANCE_DB =
        serviceDb("finance_db");
    protected static final PostgreSQLContainer<?> NOTIFICATION_DB =
        serviceDb("notification_db");

    // ─────────────────────────── Service containers ───────────────────────────

    protected static GenericContainer<?> donorService;
    protected static GenericContainer<?> receiverService;
    protected static GenericContainer<?> inventoryService;
    protected static GenericContainer<?> matchingService;
    protected static GenericContainer<?> transportService;
    protected static GenericContainer<?> verificationService;
    protected static GenericContainer<?> disasterService;
    protected static GenericContainer<?> financeService;
    protected static GenericContainer<?> notificationService;

    // ─────────────────────────── Helpers ──────────────────────────────────────

    protected static final ObjectMapper MAPPER = new ObjectMapper()
        .registerModule(new JavaTimeModule());

    /** JdbcTemplate per service DB. Populated after containers start. */
    protected static final Map<String, JdbcTemplate> DB = new ConcurrentHashMap<>();

    private static volatile boolean started = false;

    // ─────────────────────────── Lifecycle ────────────────────────────────────

    @BeforeAll
    static void startInfrastructure() {
        if (started) return;
        started = true;

        log.info("Starting shared infrastructure containers...");

        // Start infrastructure first in parallel
        Startables.deepStart(KAFKA, REDIS,
            DONOR_DB, RECEIVER_DB, INVENTORY_DB, MATCHING_DB,
            TRANSPORT_DB, VERIFICATION_DB, DISASTER_DB, FINANCE_DB, NOTIFICATION_DB
        ).join();

        log.info("Infrastructure ready. Starting domain services...");

        String bootstrapServers = KAFKA.getNetworkAliases().get(0) + ":9092";

        // ── Domain service containers ──────────────────────────────────────────
        disasterService = buildServiceContainer("disaster-service", DISASTER_PORT,
            Map.of(
                "SPRING_DATASOURCE_URL",            jdbcUrl(DISASTER_DB, "disaster_db"),
                "SPRING_DATASOURCE_USERNAME",       DISASTER_DB.getUsername(),
                "SPRING_DATASOURCE_PASSWORD",       DISASTER_DB.getPassword(),
                "SPRING_KAFKA_BOOTSTRAP_SERVERS",   bootstrapServers,
                "EUREKA_CLIENT_ENABLED",            "false",
                "SPRING_SECURITY_ENABLED",          "false"
            ));

        donorService = buildServiceContainer("donor-service", DONOR_PORT,
            Map.of(
                "SPRING_DATASOURCE_URL",            jdbcUrl(DONOR_DB, "donor_db"),
                "SPRING_DATASOURCE_USERNAME",       DONOR_DB.getUsername(),
                "SPRING_DATASOURCE_PASSWORD",       DONOR_DB.getPassword(),
                "SPRING_KAFKA_BOOTSTRAP_SERVERS",   bootstrapServers,
                "EUREKA_CLIENT_ENABLED",            "false",
                "SPRING_SECURITY_ENABLED",          "false"
            ));

        receiverService = buildServiceContainer("receiver-service", RECEIVER_PORT,
            Map.of(
                "SPRING_DATASOURCE_URL",            jdbcUrl(RECEIVER_DB, "receiver_db"),
                "SPRING_DATASOURCE_USERNAME",       RECEIVER_DB.getUsername(),
                "SPRING_DATASOURCE_PASSWORD",       RECEIVER_DB.getPassword(),
                "SPRING_KAFKA_BOOTSTRAP_SERVERS",   bootstrapServers,
                "EUREKA_CLIENT_ENABLED",            "false",
                "SPRING_SECURITY_ENABLED",          "false"
            ));

        verificationService = buildServiceContainer("verification-service", VERIFICATION_PORT,
            Map.of(
                "SPRING_DATASOURCE_URL",            jdbcUrl(VERIFICATION_DB, "verification_db"),
                "SPRING_DATASOURCE_USERNAME",       VERIFICATION_DB.getUsername(),
                "SPRING_DATASOURCE_PASSWORD",       VERIFICATION_DB.getPassword(),
                "SPRING_KAFKA_BOOTSTRAP_SERVERS",   bootstrapServers,
                "EUREKA_CLIENT_ENABLED",            "false",
                "SPRING_SECURITY_ENABLED",          "false"
            ));

        inventoryService = buildServiceContainer("inventory-service", INVENTORY_PORT,
            Map.of(
                "SPRING_DATASOURCE_URL",            jdbcUrl(INVENTORY_DB, "inventory_db"),
                "SPRING_DATASOURCE_USERNAME",       INVENTORY_DB.getUsername(),
                "SPRING_DATASOURCE_PASSWORD",       INVENTORY_DB.getPassword(),
                "SPRING_KAFKA_BOOTSTRAP_SERVERS",   bootstrapServers,
                "EUREKA_CLIENT_ENABLED",            "false",
                "SPRING_SECURITY_ENABLED",          "false"
            ));

        matchingService = buildServiceContainer("matching-service", MATCHING_PORT,
            Map.of(
                "SPRING_DATASOURCE_URL",            jdbcUrl(MATCHING_DB, "matching_db"),
                "SPRING_DATASOURCE_USERNAME",       MATCHING_DB.getUsername(),
                "SPRING_DATASOURCE_PASSWORD",       MATCHING_DB.getPassword(),
                "SPRING_KAFKA_BOOTSTRAP_SERVERS",   bootstrapServers,
                "EUREKA_CLIENT_ENABLED",            "false",
                "SPRING_SECURITY_ENABLED",          "false",
                "FEIGN_CLIENT_INVENTORY_URL",
                    "http://" + inventoryService.getNetworkAliases().get(0) + ":" + INVENTORY_PORT
            ));

        financeService = buildServiceContainer("finance-service", FINANCE_PORT,
            Map.of(
                "SPRING_DATASOURCE_URL",            jdbcUrl(FINANCE_DB, "finance_db"),
                "SPRING_DATASOURCE_USERNAME",       FINANCE_DB.getUsername(),
                "SPRING_DATASOURCE_PASSWORD",       FINANCE_DB.getPassword(),
                "SPRING_KAFKA_BOOTSTRAP_SERVERS",   bootstrapServers,
                "EUREKA_CLIENT_ENABLED",            "false",
                "SPRING_SECURITY_ENABLED",          "false"
            ));

        transportService = buildServiceContainer("transport-service", TRANSPORT_PORT,
            Map.of(
                "SPRING_DATASOURCE_URL",            jdbcUrl(TRANSPORT_DB, "transport_db"),
                "SPRING_DATASOURCE_USERNAME",       TRANSPORT_DB.getUsername(),
                "SPRING_DATASOURCE_PASSWORD",       TRANSPORT_DB.getPassword(),
                "SPRING_KAFKA_BOOTSTRAP_SERVERS",   bootstrapServers,
                "EUREKA_CLIENT_ENABLED",            "false",
                "SPRING_SECURITY_ENABLED",          "false",
                "FEIGN_CLIENT_DISASTER_URL",
                    "http://" + disasterService.getNetworkAliases().get(0) + ":" + DISASTER_PORT
            ));

        notificationService = buildServiceContainer("notification-service", NOTIFICATION_PORT,
            Map.of(
                "SPRING_DATASOURCE_URL",            jdbcUrl(NOTIFICATION_DB, "notification_db"),
                "SPRING_DATASOURCE_USERNAME",       NOTIFICATION_DB.getUsername(),
                "SPRING_DATASOURCE_PASSWORD",       NOTIFICATION_DB.getPassword(),
                "SPRING_KAFKA_BOOTSTRAP_SERVERS",   bootstrapServers,
                "EUREKA_CLIENT_ENABLED",            "false",
                "SPRING_SECURITY_ENABLED",          "false"
            ));

        // Start all services in parallel
        Startables.deepStart(
            disasterService, donorService, receiverService, verificationService,
            inventoryService, matchingService, financeService, transportService,
            notificationService
        ).join();

        log.info("All domain services started.");

        // ── Build per-service JdbcTemplates for direct DB assertions ──────────
        DB.put("donor",        jdbcTemplate(DONOR_DB));
        DB.put("receiver",     jdbcTemplate(RECEIVER_DB));
        DB.put("inventory",    jdbcTemplate(INVENTORY_DB));
        DB.put("matching",     jdbcTemplate(MATCHING_DB));
        DB.put("transport",    jdbcTemplate(TRANSPORT_DB));
        DB.put("verification", jdbcTemplate(VERIFICATION_DB));
        DB.put("disaster",     jdbcTemplate(DISASTER_DB));
        DB.put("finance",      jdbcTemplate(FINANCE_DB));
        DB.put("notification", jdbcTemplate(NOTIFICATION_DB));
    }

    // ─────────────────────────── REST helpers ─────────────────────────────────

    /** Pre-configured REST-Assured spec for a given service container. */
    protected static RequestSpecification specFor(GenericContainer<?> container, int port) {
        return RestAssured.given()
            .baseUri("http://" + container.getHost())
            .port(container.getMappedPort(port))
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON);
    }

    protected static RequestSpecification donor()        { return specFor(donorService,        DONOR_PORT); }
    protected static RequestSpecification receiver()     { return specFor(receiverService,     RECEIVER_PORT); }
    protected static RequestSpecification inventory()    { return specFor(inventoryService,    INVENTORY_PORT); }
    protected static RequestSpecification matching()     { return specFor(matchingService,     MATCHING_PORT); }
    protected static RequestSpecification transport()    { return specFor(transportService,    TRANSPORT_PORT); }
    protected static RequestSpecification verification() { return specFor(verificationService, VERIFICATION_PORT); }
    protected static RequestSpecification disaster()     { return specFor(disasterService,     DISASTER_PORT); }
    protected static RequestSpecification finance()      { return specFor(financeService,      FINANCE_PORT); }
    protected static RequestSpecification notification() { return specFor(notificationService, NOTIFICATION_PORT); }

    // ─────────────────────────── DB helpers ───────────────────────────────────

    /**
     * Queries the given service's DB for a single string column value.
     * Example: {@code dbQuery("inventory", "SELECT status FROM reservations WHERE allocation_id = ?", allocationId)}
     */
    protected static String dbQuery(String service, String sql, Object... args) {
        return DB.get(service).queryForObject(sql, String.class, args);
    }

    protected static int dbCount(String service, String sql, Object... args) {
        Integer count = DB.get(service).queryForObject(sql, Integer.class, args);
        return count == null ? 0 : count;
    }

    // ─────────────────────────── Factory helpers ──────────────────────────────

    @SuppressWarnings("resource")
    private static PostgreSQLContainer<?> serviceDb(String dbName) {
        return new PostgreSQLContainer<>(DockerImageName.parse(POSTGRES_IMAGE))
            .withNetwork(NETWORK)
            .withNetworkAliases(dbName.replace("_", "-"))
            .withDatabaseName(dbName)
            .withUsername("drn")
            .withPassword("drn")
            .withStartupTimeout(Duration.ofSeconds(60));
    }

    @SuppressWarnings("resource")
    private static GenericContainer<?> buildServiceContainer(
        String artifactId, int port, Map<String, String> envVars
    ) {
        GenericContainer<?> container =
            new GenericContainer<>(DockerImageName.parse(SVC_IMAGE_PREFIX + "/" + artifactId + ":latest"))
                .withNetwork(NETWORK)
                .withNetworkAliases(artifactId)
                .withExposedPorts(port)
                .withEnv(envVars)
                .waitingFor(
                    Wait.forHttp("/actuator/health")
                        .forPort(port)
                        .forStatusCode(200)
                        .withStartupTimeout(Duration.ofSeconds(120))
                );
        return container;
    }

    private static String jdbcUrl(PostgreSQLContainer<?> db, String dbName) {
        // Inside the Docker network use the alias; outside use the mapped port
        return "jdbc:postgresql://" + db.getNetworkAliases().get(0) + ":5432/" + dbName;
    }

    private static JdbcTemplate jdbcTemplate(PostgreSQLContainer<?> db) {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("org.postgresql.Driver");
        ds.setUrl(db.getJdbcUrl());
        ds.setUsername(db.getUsername());
        ds.setPassword(db.getPassword());
        return new JdbcTemplate(ds);
    }
}
