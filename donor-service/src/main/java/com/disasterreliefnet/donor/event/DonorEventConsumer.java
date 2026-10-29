package com.disasterreliefnet.donor.event;

import com.disasterreliefnet.common.dto.EventEnvelope;
import com.disasterreliefnet.common.enums.DonationStatus;
import com.disasterreliefnet.common.event.KafkaTopics;
import com.disasterreliefnet.common.event.payload.PaymentCompletedPayload;
import com.disasterreliefnet.common.event.payload.ShipmentDeliveredPayload;
import com.disasterreliefnet.common.idempotency.IdempotentEventConsumer;
import com.disasterreliefnet.donor.domain.Donor;
import com.disasterreliefnet.donor.repository.DonorRepository;
import com.disasterreliefnet.donor.service.DonationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Idempotent Kafka consumer listening for payment completions and shipment deliveries to update donor impact.
 */
@Component
public class DonorEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(DonorEventConsumer.class);

    private final DonationService donationService;
    private final DonorRepository donorRepository;
    private final IdempotentEventConsumer idempotentEventConsumer;
    private final ObjectMapper objectMapper;

    public DonorEventConsumer(
        DonationService donationService,
        DonorRepository donorRepository,
        IdempotentEventConsumer idempotentEventConsumer,
        ObjectMapper objectMapper
    ) {
        this.donationService = donationService;
        this.donorRepository = donorRepository;
        this.idempotentEventConsumer = idempotentEventConsumer;
        this.objectMapper = objectMapper;
    }

    /**
     * Consumes PaymentCompleted event from Finance Service, marks donation as PAID, and generates receipt.
     */
    @KafkaListener(topics = KafkaTopics.PAYMENT_COMPLETED, groupId = "${spring.kafka.consumer.group-id:donor-service-group}")
    public void handlePaymentCompleted(String message) {
        try {
            EventEnvelope<PaymentCompletedPayload> envelope = objectMapper.readValue(
                message,
                new TypeReference<>() {}
            );

            idempotentEventConsumer.execute(envelope.eventId(), envelope.eventType(), () -> {
                PaymentCompletedPayload payload = envelope.payload();
                log.info("Processing PaymentCompleted event: paymentId={}, donationId={}, amount={}{}",
                    payload.paymentId(), payload.donationId(), payload.currency(), payload.amount());

                donationService.transitionDonationStatus(
                    payload.donationId(),
                    DonationStatus.PAID,
                    "Payment confirmed via gateway: " + payload.gatewayReference()
                );
            });
        } catch (Exception e) {
            log.error("Failed to process PaymentCompleted event: {}", message, e);
            throw new RuntimeException("Error processing PaymentCompleted event", e);
        }
    }

    /**
     * Consumes ShipmentDelivered event from Transport Service to update donor impact metrics.
     */
    @KafkaListener(topics = KafkaTopics.SHIPMENT_DELIVERED, groupId = "${spring.kafka.consumer.group-id:donor-service-group}")
    public void handleShipmentDelivered(String message) {
        try {
            EventEnvelope<ShipmentDeliveredPayload> envelope = objectMapper.readValue(
                message,
                new TypeReference<>() {}
            );

            idempotentEventConsumer.execute(envelope.eventId(), envelope.eventType(), () -> {
                ShipmentDeliveredPayload payload = envelope.payload();
                log.info("Processing ShipmentDelivered event: shipmentId={}, allocationId={}, deliveredAt={}",
                    payload.shipmentId(), payload.allocationId(), payload.deliveredAt());

                // Update active donors with increased impact deliveries count
                List<Donor> activeDonors = donorRepository.findAll();
                for (Donor donor : activeDonors) {
                    donor.incrementImpactDeliveries();
                }
                donorRepository.saveAll(activeDonors);
                log.info("Incremented impact deliveries count for {} donors", activeDonors.size());
            });
        } catch (Exception e) {
            log.error("Failed to process ShipmentDelivered event: {}", message, e);
            throw new RuntimeException("Error processing ShipmentDelivered event", e);
        }
    }
}
