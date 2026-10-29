package com.disasterreliefnet.inventory.repository;

import com.disasterreliefnet.common.enums.AidCategory;
import com.disasterreliefnet.common.enums.ReservationStatus;
import com.disasterreliefnet.inventory.domain.Item;
import com.disasterreliefnet.inventory.domain.Reservation;
import com.disasterreliefnet.inventory.domain.Warehouse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class InventoryRepositoryIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("inventory_db")
        .withUsername("postgres")
        .withPassword("postgres");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Test
    void shouldPersistAndRetrieveWarehouseWithVersionTracking() {
        UUID warehouseId = UUID.randomUUID();
        Warehouse warehouse = Warehouse.builder()
            .warehouseId(warehouseId)
            .name("Atlantic Relief Hub")
            .street("444 Coastal Hwy")
            .city("Savannah")
            .state("GA")
            .country("US")
            .zip("31401")
            .capacityCbm(new BigDecimal("7500.00"))
            .active(true)
            .build();

        Warehouse saved = warehouseRepository.saveAndFlush(warehouse);
        assertNotNull(saved.getVersion());
        assertEquals(0L, saved.getVersion());

        Optional<Warehouse> found = warehouseRepository.findByWarehouseIdAndDeletedAtIsNull(warehouseId);
        assertTrue(found.isPresent());
        assertEquals("Atlantic Relief Hub", found.get().getName());
    }

    @Test
    void shouldPersistItemAndEnforceOptimisticLocking() {
        Warehouse warehouse = warehouseRepository.saveAndFlush(
            Warehouse.builder()
                .warehouseId(UUID.randomUUID())
                .name("Southern Depot")
                .active(true)
                .build()
        );

        UUID itemId = UUID.randomUUID();
        Item item = Item.builder()
            .itemId(itemId)
            .warehouseId(warehouse.getWarehouseId())
            .name("Medical Oxygen Canister")
            .category(AidCategory.MEDICINE)
            .unit("canisters")
            .quantityOnHand(new BigDecimal("200.0"))
            .quantityReserved(new BigDecimal("10.0"))
            .expiresAt(Instant.now().plus(365, ChronoUnit.DAYS))
            .build();

        Item saved = itemRepository.saveAndFlush(item);
        assertEquals(0L, saved.getVersion());
        assertEquals(new BigDecimal("190.0"), saved.getQuantityAvailable());

        saved.setQuantityOnHand(new BigDecimal("250.0"));
        Item updated = itemRepository.saveAndFlush(saved);
        assertEquals(1L, updated.getVersion());
    }

    @Test
    void shouldCreateAndQueryReservationsByAllocation() {
        Warehouse warehouse = warehouseRepository.saveAndFlush(
            Warehouse.builder()
                .warehouseId(UUID.randomUUID())
                .name("Gulf Coast Depot")
                .active(true)
                .build()
        );

        Item item = itemRepository.saveAndFlush(
            Item.builder()
                .itemId(UUID.randomUUID())
                .warehouseId(warehouse.getWarehouseId())
                .name("Clean Drinking Water 5L")
                .category(AidCategory.WATER)
                .unit("jugs")
                .quantityOnHand(new BigDecimal("500.0"))
                .quantityReserved(new BigDecimal("50.0"))
                .build()
        );

        UUID allocationId = UUID.randomUUID();
        Reservation reservation = Reservation.builder()
            .reservationId(UUID.randomUUID())
            .itemId(item.getItemId())
            .allocationId(allocationId)
            .quantity(new BigDecimal("50.0"))
            .status(ReservationStatus.CONFIRMED)
            .build();

        reservationRepository.saveAndFlush(reservation);

        Optional<Reservation> found = reservationRepository.findByAllocationIdAndDeletedAtIsNull(allocationId);
        assertTrue(found.isPresent());
        assertEquals(new BigDecimal("50.0"), found.get().getQuantity());
        assertEquals(ReservationStatus.CONFIRMED, found.get().getStatus());
    }
}
