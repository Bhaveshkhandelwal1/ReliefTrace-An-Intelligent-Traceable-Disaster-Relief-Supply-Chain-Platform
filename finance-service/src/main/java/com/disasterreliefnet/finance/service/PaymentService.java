package com.disasterreliefnet.finance.service;

import com.disasterreliefnet.common.enums.DonationType;
import com.disasterreliefnet.common.enums.PaymentStatus;
import com.disasterreliefnet.common.event.KafkaTopics;
import com.disasterreliefnet.common.event.payload.DonationRegisteredPayload;
import com.disasterreliefnet.common.event.payload.PaymentCompletedPayload;
import com.disasterreliefnet.common.exception.BusinessRuleException;
import com.disasterreliefnet.common.exception.ResourceNotFoundException;
import com.disasterreliefnet.common.outbox.OutboxService;
import com.disasterreliefnet.finance.client.PaymentGatewayStub;
import com.disasterreliefnet.finance.domain.LedgerEntryType;
import com.disasterreliefnet.finance.domain.PaymentRecord;
import com.disasterreliefnet.finance.dto.PaymentRecordResponse;
import com.disasterreliefnet.finance.dto.ProcessPaymentRequest;
import com.disasterreliefnet.finance.mapper.FinanceMapper;
import com.disasterreliefnet.finance.repository.PaymentRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Service orchestrating monetary donation payment records, gateway transactions, and reconciliation.
 */
@Service
@Transactional(readOnly = true)
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRecordRepository paymentRecordRepository;
    private final PaymentGatewayStub paymentGatewayStub;
    private final LedgerService ledgerService;
    private final OutboxService outboxService;
    private final FinanceMapper financeMapper;

    public PaymentService(
        PaymentRecordRepository paymentRecordRepository,
        PaymentGatewayStub paymentGatewayStub,
        LedgerService ledgerService,
        OutboxService outboxService,
        FinanceMapper financeMapper
    ) {
        this.paymentRecordRepository = paymentRecordRepository;
        this.paymentGatewayStub = paymentGatewayStub;
        this.ledgerService = ledgerService;
        this.outboxService = outboxService;
        this.financeMapper = financeMapper;
    }

    @Transactional
    public void createPaymentRecordFromDonation(DonationRegisteredPayload payload) {
        if (payload.donationType() != DonationType.MONETARY || payload.amount() == null || payload.amount().compareTo(BigDecimal.ZERO) <= 0) {
            log.info("Ignoring non-monetary or zero-amount donation {}", payload.donationId());
            return;
        }

        if (paymentRecordRepository.existsByDonationIdAndDeletedAtIsNull(payload.donationId())) {
            log.info("Payment record already exists for donation {}, skipping duplicate", payload.donationId());
            return;
        }

        PaymentRecord record = new PaymentRecord();
        record.setDonationId(payload.donationId());
        record.setDonorId(payload.donorId());
        record.setAmount(payload.amount());
        record.setCurrency(payload.currency() != null ? payload.currency() : "USD");
        record.setStatus(PaymentStatus.PENDING);

        PaymentRecord saved = paymentRecordRepository.save(record);
        log.info("Created pending payment record {} for donation {}", saved.getPaymentId(), payload.donationId());
    }

    @Transactional
    public PaymentRecordResponse processPayment(UUID paymentId, ProcessPaymentRequest request) {
        log.info("Processing payment ID: {}", paymentId);

        PaymentRecord record = paymentRecordRepository.findByPaymentIdAndDeletedAtIsNull(paymentId)
            .orElseThrow(() -> new ResourceNotFoundException("PaymentRecord", paymentId));

        if (record.getStatus() == PaymentStatus.COMPLETED) {
            throw new BusinessRuleException("Payment " + paymentId + " has already been completed");
        }

        record.setStatus(PaymentStatus.PROCESSING);
        paymentRecordRepository.save(record);

        PaymentGatewayStub.GatewayChargeResult chargeResult = paymentGatewayStub.processCharge(
            record.getAmount(),
            record.getCurrency(),
            request.gatewayToken()
        );

        if (!chargeResult.success()) {
            record.setStatus(PaymentStatus.FAILED);
            paymentRecordRepository.save(record);
            throw new BusinessRuleException("Payment charge failed: " + chargeResult.errorMessage());
        }

        Instant processedAt = Instant.now();
        record.setStatus(PaymentStatus.COMPLETED);
        record.setGatewayReference(chargeResult.transactionReference());
        record.setProcessedAt(processedAt);
        PaymentRecord saved = paymentRecordRepository.save(record);

        // Record credit to platform general donations ledger
        ledgerService.recordEntry(
            LedgerEntryType.DONATION_IN,
            saved.getAmount(),
            saved.getCurrency(),
            saved.getDonationId(),
            "Monetary donation received (Gateway Ref: " + saved.getGatewayReference() + ")"
        );

        // Enqueue PaymentCompleted event to Kafka outbox
        PaymentCompletedPayload payload = new PaymentCompletedPayload(
            saved.getPaymentId(),
            saved.getDonationId(),
            saved.getDonorId(),
            saved.getAmount(),
            saved.getCurrency(),
            saved.getGatewayReference(),
            processedAt
        );

        outboxService.enqueue(
            KafkaTopics.PAYMENT_COMPLETED,
            "PAYMENT",
            saved.getPaymentId(),
            "PaymentCompleted",
            payload,
            null
        );

        log.info("Payment {} completed successfully with gateway reference {}", saved.getPaymentId(), saved.getGatewayReference());
        return financeMapper.toPaymentRecordResponse(saved);
    }

    public PaymentRecordResponse getPaymentById(UUID paymentId) {
        PaymentRecord record = paymentRecordRepository.findByPaymentIdAndDeletedAtIsNull(paymentId)
            .orElseThrow(() -> new ResourceNotFoundException("PaymentRecord", paymentId));
        return financeMapper.toPaymentRecordResponse(record);
    }

    public Page<PaymentRecordResponse> findPayments(UUID donationId, PaymentStatus status, Pageable pageable) {
        if (donationId != null) {
            return paymentRecordRepository.findAllByDonationIdAndDeletedAtIsNull(donationId, pageable)
                .map(financeMapper::toPaymentRecordResponse);
        } else if (status != null) {
            return paymentRecordRepository.findAllByStatusAndDeletedAtIsNull(status, pageable)
                .map(financeMapper::toPaymentRecordResponse);
        }
        return paymentRecordRepository.findAllByDeletedAtIsNull(pageable)
            .map(financeMapper::toPaymentRecordResponse);
    }
}
