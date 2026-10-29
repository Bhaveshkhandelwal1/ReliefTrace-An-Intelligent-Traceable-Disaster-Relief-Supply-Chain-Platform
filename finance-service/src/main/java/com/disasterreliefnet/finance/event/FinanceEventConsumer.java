package com.disasterreliefnet.finance.event;

import com.disasterreliefnet.common.dto.EventEnvelope;
import com.disasterreliefnet.common.event.KafkaTopics;
import com.disasterreliefnet.common.event.payload.AllocationApprovedPayload;
import com.disasterreliefnet.common.event.payload.DonationRegisteredPayload;
import com.disasterreliefnet.common.idempotency.IdempotentEventConsumer;
import com.disasterreliefnet.finance.service.DisbursementService;
import com.disasterreliefnet.finance.service.PaymentService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Idempotent Kafka consumer listening for incoming donation registrations and approved allocations.
 */
@Component
public class FinanceEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(FinanceEventConsumer.class);

    private final PaymentService paymentService;
    private final DisbursementService disbursementService;
    private final IdempotentEventConsumer idempotentEventConsumer;
    private final ObjectMapper objectMapper;

    public FinanceEventConsumer(
        PaymentService paymentService,
        DisbursementService disbursementService,
        IdempotentEventConsumer idempotentEventConsumer,
        ObjectMapper objectMapper
    ) {
        this.paymentService = paymentService;
        this.disbursementService = disbursementService;
        this.idempotentEventConsumer = idempotentEventConsumer;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = KafkaTopics.DONATION_REGISTERED, groupId = "${spring.kafka.consumer.group-id:finance-service-group}")
    public void handleDonationRegistered(String message) {
        try {
            EventEnvelope<DonationRegisteredPayload> envelope = objectMapper.readValue(
                message,
                new TypeReference<>() {}
            );

            idempotentEventConsumer.execute(envelope.eventId(), envelope.eventType(), () -> {
                log.info("Handling DonationRegistered event {} for donation {}", envelope.eventId(), envelope.payload().donationId());
                paymentService.createPaymentRecordFromDonation(envelope.payload());
            });
        } catch (Exception ex) {
            log.error("Failed to process DonationRegistered event: {}", ex.getMessage(), ex);
            throw new IllegalArgumentException("Invalid DonationRegistered event payload", ex);
        }
    }

    @KafkaListener(topics = KafkaTopics.ALLOCATION_APPROVED, groupId = "${spring.kafka.consumer.group-id:finance-service-group}")
    public void handleAllocationApproved(String message) {
        try {
            EventEnvelope<AllocationApprovedPayload> envelope = objectMapper.readValue(
                message,
                new TypeReference<>() {}
            );

            idempotentEventConsumer.execute(envelope.eventId(), envelope.eventType(), () -> {
                log.info("Handling AllocationApproved event {} for allocation {}", envelope.eventId(), envelope.payload().allocationId());
                disbursementService.createDisbursementFromAllocation(envelope.payload());
            });
        } catch (Exception ex) {
            log.error("Failed to process AllocationApproved event: {}", ex.getMessage(), ex);
            throw new IllegalArgumentException("Invalid AllocationApproved event payload", ex);
        }
    }
}
