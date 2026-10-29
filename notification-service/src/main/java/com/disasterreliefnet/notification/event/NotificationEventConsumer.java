package com.disasterreliefnet.notification.event;

import com.disasterreliefnet.common.dto.EventEnvelope;
import com.disasterreliefnet.common.event.KafkaTopics;
import com.disasterreliefnet.common.event.payload.*;
import com.disasterreliefnet.common.idempotency.IdempotentEventConsumer;
import com.disasterreliefnet.notification.service.NotificationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * High-throughput Kafka consumer fanning out platform notifications across communication channels.
 */
@Component
public class NotificationEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventConsumer.class);

    private final NotificationService notificationService;
    private final IdempotentEventConsumer idempotentEventConsumer;
    private final ObjectMapper objectMapper;

    public NotificationEventConsumer(
        NotificationService notificationService,
        IdempotentEventConsumer idempotentEventConsumer,
        ObjectMapper objectMapper
    ) {
        this.notificationService = notificationService;
        this.idempotentEventConsumer = idempotentEventConsumer;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = KafkaTopics.DONATION_REGISTERED, groupId = "${spring.kafka.consumer.group-id:notification-service-group}")
    public void handleDonationRegistered(String message) {
        try {
            EventEnvelope<DonationRegisteredPayload> envelope = objectMapper.readValue(message, new TypeReference<>() {});
            idempotentEventConsumer.execute(envelope.eventId(), envelope.eventType(), () -> {
                DonationRegisteredPayload payload = envelope.payload();
                String body = String.format("Dear %s, thank you for your generous donation (%s) to DisasterReliefNet.",
                    payload.donorName(), payload.donationType());
                notificationService.dispatch(payload.donorId().toString(), "Donation Received", body, envelope.eventId(), envelope.eventType());
            });
        } catch (Exception ex) {
            log.error("Failed to process DonationRegistered event: {}", ex.getMessage(), ex);
        }
    }

    @KafkaListener(topics = KafkaTopics.REQUEST_CREATED, groupId = "${spring.kafka.consumer.group-id:notification-service-group}")
    public void handleRequestCreated(String message) {
        try {
            EventEnvelope<RequestCreatedPayload> envelope = objectMapper.readValue(message, new TypeReference<>() {});
            idempotentEventConsumer.execute(envelope.eventId(), envelope.eventType(), () -> {
                RequestCreatedPayload payload = envelope.payload();
                String body = "Your emergency relief request has been received and queued for priority verification.";
                notificationService.dispatch(payload.receiverId().toString(), "Aid Request Received", body, envelope.eventId(), envelope.eventType());
            });
        } catch (Exception ex) {
            log.error("Failed to process RequestCreated event: {}", ex.getMessage(), ex);
        }
    }

    @KafkaListener(topics = KafkaTopics.REQUEST_VERIFIED, groupId = "${spring.kafka.consumer.group-id:notification-service-group}")
    public void handleRequestVerified(String message) {
        try {
            EventEnvelope<RequestVerifiedPayload> envelope = objectMapper.readValue(message, new TypeReference<>() {});
            idempotentEventConsumer.execute(envelope.eventId(), envelope.eventType(), () -> {
                RequestVerifiedPayload payload = envelope.payload();
                String body = "Your relief request has been verified by our field coordination team and is queued for supply matching.";
                notificationService.dispatch(payload.receiverId().toString(), "Aid Request Verified", body, envelope.eventId(), envelope.eventType());
            });
        } catch (Exception ex) {
            log.error("Failed to process RequestVerified event: {}", ex.getMessage(), ex);
        }
    }

    @KafkaListener(topics = KafkaTopics.ALLOCATION_APPROVED, groupId = "${spring.kafka.consumer.group-id:notification-service-group}")
    public void handleAllocationApproved(String message) {
        try {
            EventEnvelope<AllocationApprovedPayload> envelope = objectMapper.readValue(message, new TypeReference<>() {});
            idempotentEventConsumer.execute(envelope.eventId(), envelope.eventType(), () -> {
                AllocationApprovedPayload payload = envelope.payload();
                String body = String.format("Aid supplies of %s %s have been allocated and approved for dispatch.",
                    payload.quantity(), payload.unit());
                notificationService.dispatch(payload.receiverId().toString(), "Supplies Allocated", body, envelope.eventId(), envelope.eventType());
            });
        } catch (Exception ex) {
            log.error("Failed to process AllocationApproved event: {}", ex.getMessage(), ex);
        }
    }

    @KafkaListener(topics = KafkaTopics.SHIPMENT_ASSIGNED, groupId = "${spring.kafka.consumer.group-id:notification-service-group}")
    public void handleShipmentAssigned(String message) {
        try {
            EventEnvelope<ShipmentAssignedPayload> envelope = objectMapper.readValue(message, new TypeReference<>() {});
            idempotentEventConsumer.execute(envelope.eventId(), envelope.eventType(), () -> {
                ShipmentAssignedPayload payload = envelope.payload();
                String receiverBody = String.format("A driver (%s, Vehicle: %s) has been assigned to deliver your relief supplies.",
                    payload.driverName(), payload.vehiclePlate());
                notificationService.dispatch(payload.receiverId().toString(), "Driver Dispatched", receiverBody, envelope.eventId(), envelope.eventType());

                String driverBody = String.format("You have been assigned shipment %s to destination: %s, %s.",
                    payload.shipmentId(), payload.deliveryAddress().city(), payload.deliveryAddress().state());
                notificationService.dispatch(payload.driverId().toString(), "New Shipment Assignment", driverBody, envelope.eventId(), envelope.eventType());
            });
        } catch (Exception ex) {
            log.error("Failed to process ShipmentAssigned event: {}", ex.getMessage(), ex);
        }
    }

    @KafkaListener(topics = KafkaTopics.SHIPMENT_DELIVERED, groupId = "${spring.kafka.consumer.group-id:notification-service-group}")
    public void handleShipmentDelivered(String message) {
        try {
            EventEnvelope<ShipmentDeliveredPayload> envelope = objectMapper.readValue(message, new TypeReference<>() {});
            idempotentEventConsumer.execute(envelope.eventId(), envelope.eventType(), () -> {
                ShipmentDeliveredPayload payload = envelope.payload();
                String body = "Your relief shipment has been successfully delivered and confirmed on the network.";
                notificationService.dispatch(payload.receiverId().toString(), "Shipment Delivered", body, envelope.eventId(), envelope.eventType());
            });
        } catch (Exception ex) {
            log.error("Failed to process ShipmentDelivered event: {}", ex.getMessage(), ex);
        }
    }

    @KafkaListener(topics = KafkaTopics.PAYMENT_COMPLETED, groupId = "${spring.kafka.consumer.group-id:notification-service-group}")
    public void handlePaymentCompleted(String message) {
        try {
            EventEnvelope<PaymentCompletedPayload> envelope = objectMapper.readValue(message, new TypeReference<>() {});
            idempotentEventConsumer.execute(envelope.eventId(), envelope.eventType(), () -> {
                PaymentCompletedPayload payload = envelope.payload();
                String body = String.format("Your payment of %s %s has completed successfully (Ref: %s). Tax receipt issued.",
                    payload.amount(), payload.currency(), payload.gatewayReference());
                notificationService.dispatch(payload.donorId().toString(), "Payment Receipt Confirmed", body, envelope.eventId(), envelope.eventType());
            });
        } catch (Exception ex) {
            log.error("Failed to process PaymentCompleted event: {}", ex.getMessage(), ex);
        }
    }
}
