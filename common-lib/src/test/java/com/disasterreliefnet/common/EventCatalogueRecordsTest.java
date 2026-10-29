package com.disasterreliefnet.common;

import com.disasterreliefnet.common.dto.AffectedAreaDto;
import com.disasterreliefnet.common.dto.DeliveryAddressDto;
import com.disasterreliefnet.common.enums.*;
import com.disasterreliefnet.common.event.KafkaTopics;
import com.disasterreliefnet.common.event.payload.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EventCatalogueRecordsTest {

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void shouldValidateKafkaTopicsConstants() {
        assertEquals("donation.registered", KafkaTopics.DONATION_REGISTERED);
        assertEquals("request.created", KafkaTopics.REQUEST_CREATED);
        assertEquals("request.verified", KafkaTopics.REQUEST_VERIFIED);
        assertEquals("allocation.approved", KafkaTopics.ALLOCATION_APPROVED);
        assertEquals("allocation.rejected", KafkaTopics.ALLOCATION_REJECTED);
        assertEquals("stock.reserved", KafkaTopics.STOCK_RESERVED);
        assertEquals("stock.reservation.failed", KafkaTopics.STOCK_RESERVATION_FAILED);
        assertEquals("shipment.assigned", KafkaTopics.SHIPMENT_ASSIGNED);
        assertEquals("shipment.picked_up", KafkaTopics.SHIPMENT_PICKED_UP);
        assertEquals("shipment.delivered", KafkaTopics.SHIPMENT_DELIVERED);
        assertEquals("delivery.disputed", KafkaTopics.DELIVERY_DISPUTED);
        assertEquals("payment.completed", KafkaTopics.PAYMENT_COMPLETED);
        assertEquals("disaster.declared", KafkaTopics.DISASTER_DECLARED);
        assertEquals("disaster.updated", KafkaTopics.DISASTER_UPDATED);
        assertEquals("disaster.closed", KafkaTopics.DISASTER_CLOSED);
    }

    @Test
    void shouldCreateAndSerializeAll15EventPayloadRecords() throws Exception {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();
        DeliveryAddressDto addr = new DeliveryAddressDto("100 Main St", "City", "State", "US", "12345");
        AffectedAreaDto area = new AffectedAreaDto(12.34, 56.78, 50.0, "Region", "US");
        Instant now = Instant.now();

        // 1. DonationRegistered
        DonationRegisteredPayload p1 = new DonationRegisteredPayload(
            id1, id2, DonationType.MONETARY, BigDecimal.valueOf(100), "USD", null, null, id3, "donor@test.com", "John"
        );
        assertNotNull(mapper.writeValueAsString(p1));

        // 2. RequestCreated
        RequestCreatedPayload p2 = new RequestCreatedPayload(
            id1, id2, id3, AidCategory.FOOD, "Rice", BigDecimal.valueOf(50), "kg", UrgencyLevel.HIGH, addr, "rec@test.com", "Jane"
        );
        assertNotNull(mapper.writeValueAsString(p2));

        // 3. RequestVerified
        RequestVerifiedPayload p3 = new RequestVerifiedPayload(id1, id2, id3, "APPROVED", "coord1", "Verified");
        assertNotNull(mapper.writeValueAsString(p3));

        // 4. AllocationApproved
        AllocationApprovedPayload p4 = new AllocationApprovedPayload(
            id1, id2, id3, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), BigDecimal.valueOf(50), "kg", "admin", addr
        );
        assertNotNull(mapper.writeValueAsString(p4));

        // 5. AllocationRejected
        AllocationRejectedPayload p5 = new AllocationRejectedPayload(id1, id2, id3, "No stock", "coord1");
        assertNotNull(mapper.writeValueAsString(p5));

        // 6. StockReserved
        StockReservedPayload p6 = new StockReservedPayload(
            id1, id2, id3, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), BigDecimal.valueOf(50), "kg", addr, addr
        );
        assertNotNull(mapper.writeValueAsString(p6));

        // 7. StockReservationFailed
        StockReservationFailedPayload p7 = new StockReservationFailedPayload(
            id1, id2, id3, BigDecimal.valueOf(50), BigDecimal.valueOf(10), FailureReason.INSUFFICIENT_STOCK
        );
        assertNotNull(mapper.writeValueAsString(p7));

        // 8. ShipmentAssigned
        ShipmentAssignedPayload p8 = new ShipmentAssignedPayload(
            id1, id2, id3, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Dave", "XYZ-123", now, addr
        );
        assertNotNull(mapper.writeValueAsString(p8));

        // 9. ShipmentPickedUp
        ShipmentPickedUpPayload p9 = new ShipmentPickedUpPayload(id1, id2, id3, UUID.randomUUID(), now);
        assertNotNull(mapper.writeValueAsString(p9));

        // 10. ShipmentDelivered
        ShipmentDeliveredPayload p10 = new ShipmentDeliveredPayload(id1, id2, id3, UUID.randomUUID(), UUID.randomUUID(), now, "https://pod");
        assertNotNull(mapper.writeValueAsString(p10));

        // 11. DeliveryDisputed
        DeliveryDisputedPayload p11 = new DeliveryDisputedPayload(id1, id2, id3, UUID.randomUUID(), "Damaged", "https://evidence", "RECEIVER");
        assertNotNull(mapper.writeValueAsString(p11));

        // 12. PaymentCompleted
        PaymentCompletedPayload p12 = new PaymentCompletedPayload(id1, id2, id3, BigDecimal.valueOf(500), "USD", "gw-ref", now);
        assertNotNull(mapper.writeValueAsString(p12));

        // 13. DisasterDeclared
        DisasterDeclaredPayload p13 = new DisasterDeclaredPayload(id1, "Flood Alert", DisasterType.FLOOD, SeverityLevel.SEVERE, area, now);
        assertNotNull(mapper.writeValueAsString(p13));

        // 14. DisasterUpdated
        DisasterUpdatedPayload p14 = new DisasterUpdatedPayload(id1, SeverityLevel.CATASTROPHIC, 10000, "coord1");
        assertNotNull(mapper.writeValueAsString(p14));

        // 15. DisasterClosed
        DisasterClosedPayload p15 = new DisasterClosedPayload(id1, "Completed", "admin", now);
        assertNotNull(mapper.writeValueAsString(p15));
    }
}
