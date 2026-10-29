package com.disasterreliefnet.finance.service;

import com.disasterreliefnet.common.enums.DonationType;
import com.disasterreliefnet.common.enums.PaymentStatus;
import com.disasterreliefnet.common.event.KafkaTopics;
import com.disasterreliefnet.common.event.payload.DonationRegisteredPayload;
import com.disasterreliefnet.common.exception.BusinessRuleException;
import com.disasterreliefnet.common.exception.ResourceNotFoundException;
import com.disasterreliefnet.common.outbox.OutboxService;
import com.disasterreliefnet.finance.client.PaymentGatewayStub;
import com.disasterreliefnet.finance.domain.LedgerEntry;
import com.disasterreliefnet.finance.domain.LedgerEntryType;
import com.disasterreliefnet.finance.domain.PaymentRecord;
import com.disasterreliefnet.finance.dto.PaymentRecordResponse;
import com.disasterreliefnet.finance.dto.ProcessPaymentRequest;
import com.disasterreliefnet.finance.mapper.FinanceMapper;
import com.disasterreliefnet.finance.mapper.FinanceMapperImpl;
import com.disasterreliefnet.finance.repository.PaymentRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRecordRepository paymentRecordRepository;

    @Mock
    private PaymentGatewayStub paymentGatewayStub;

    @Mock
    private LedgerService ledgerService;

    @Mock
    private OutboxService outboxService;

    private final FinanceMapper financeMapper = new FinanceMapperImpl();
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(
            paymentRecordRepository,
            paymentGatewayStub,
            ledgerService,
            outboxService,
            financeMapper
        );
    }

    @Test
    @DisplayName("Should create payment record from monetary donation")
    void createPaymentRecordFromDonation_Success() {
        UUID donationId = UUID.randomUUID();
        UUID donorId = UUID.randomUUID();
        DonationRegisteredPayload payload = new DonationRegisteredPayload(
            donationId,
            donorId,
            DonationType.MONETARY,
            BigDecimal.valueOf(250.00),
            "USD",
            null,
            null,
            UUID.randomUUID(),
            "donor@relief.org",
            "Generous Donor"
        );

        when(paymentRecordRepository.existsByDonationIdAndDeletedAtIsNull(donationId)).thenReturn(false);
        when(paymentRecordRepository.save(any(PaymentRecord.class))).thenAnswer(i -> {
            PaymentRecord r = i.getArgument(0);
            r.setPaymentId(UUID.randomUUID());
            return r;
        });

        paymentService.createPaymentRecordFromDonation(payload);

        verify(paymentRecordRepository).save(any(PaymentRecord.class));
    }

    @Test
    @DisplayName("Should ignore in-kind donation for payment record creation")
    void createPaymentRecordFromDonation_InKind_Ignored() {
        DonationRegisteredPayload payload = new DonationRegisteredPayload(
            UUID.randomUUID(),
            UUID.randomUUID(),
            DonationType.IN_KIND,
            null,
            null,
            "Blankets",
            BigDecimal.valueOf(50),
            UUID.randomUUID(),
            "donor@relief.org",
            "InKind Donor"
        );

        paymentService.createPaymentRecordFromDonation(payload);

        verify(paymentRecordRepository, never()).save(any(PaymentRecord.class));
    }

    @Test
    @DisplayName("Should process payment, write ledger credit and emit outbox event")
    void processPayment_Success() {
        UUID paymentId = UUID.randomUUID();
        UUID donationId = UUID.randomUUID();
        PaymentRecord record = new PaymentRecord();
        record.setPaymentId(paymentId);
        record.setDonationId(donationId);
        record.setDonorId(UUID.randomUUID());
        record.setAmount(BigDecimal.valueOf(150.00));
        record.setCurrency("USD");
        record.setStatus(PaymentStatus.PENDING);

        when(paymentRecordRepository.findByPaymentIdAndDeletedAtIsNull(paymentId)).thenReturn(Optional.of(record));
        when(paymentGatewayStub.processCharge(eq(BigDecimal.valueOf(150.00)), eq("USD"), eq("tok_visa_valid")))
            .thenReturn(new PaymentGatewayStub.GatewayChargeResult(true, "ch_123456789", null));
        when(paymentRecordRepository.save(any(PaymentRecord.class))).thenAnswer(i -> i.getArgument(0));

        ProcessPaymentRequest request = new ProcessPaymentRequest("tok_visa_valid");
        PaymentRecordResponse response = paymentService.processPayment(paymentId, request);

        assertNotNull(response);
        assertEquals(PaymentStatus.COMPLETED, response.status());
        assertEquals("ch_123456789", response.gatewayReference());

        verify(ledgerService).recordEntry(
            eq(LedgerEntryType.DONATION_IN),
            eq(BigDecimal.valueOf(150.00)),
            eq("USD"),
            eq(donationId),
            contains("ch_123456789")
        );

        verify(outboxService).enqueue(
            eq(KafkaTopics.PAYMENT_COMPLETED),
            eq("PAYMENT"),
            eq(paymentId),
            eq("PaymentCompleted"),
            any(),
            isNull()
        );
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when payment already completed")
    void processPayment_AlreadyCompleted() {
        UUID paymentId = UUID.randomUUID();
        PaymentRecord record = new PaymentRecord();
        record.setPaymentId(paymentId);
        record.setStatus(PaymentStatus.COMPLETED);

        when(paymentRecordRepository.findByPaymentIdAndDeletedAtIsNull(paymentId)).thenReturn(Optional.of(record));

        ProcessPaymentRequest request = new ProcessPaymentRequest("tok_visa");
        assertThrows(BusinessRuleException.class, () -> paymentService.processPayment(paymentId, request));
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when gateway declines payment")
    void processPayment_GatewayDeclined() {
        UUID paymentId = UUID.randomUUID();
        PaymentRecord record = new PaymentRecord();
        record.setPaymentId(paymentId);
        record.setAmount(BigDecimal.valueOf(100.00));
        record.setCurrency("USD");
        record.setStatus(PaymentStatus.PENDING);

        when(paymentRecordRepository.findByPaymentIdAndDeletedAtIsNull(paymentId)).thenReturn(Optional.of(record));
        when(paymentGatewayStub.processCharge(any(), any(), eq("fail_card")))
            .thenReturn(new PaymentGatewayStub.GatewayChargeResult(false, null, "Card declined"));

        ProcessPaymentRequest request = new ProcessPaymentRequest("fail_card");
        assertThrows(BusinessRuleException.class, () -> paymentService.processPayment(paymentId, request));

        assertEquals(PaymentStatus.FAILED, record.getStatus());
        verify(outboxService, never()).enqueue(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should retrieve payment by ID")
    void getPaymentById_Success() {
        UUID paymentId = UUID.randomUUID();
        PaymentRecord record = new PaymentRecord();
        record.setPaymentId(paymentId);
        record.setAmount(BigDecimal.valueOf(500.00));
        record.setCurrency("EUR");

        when(paymentRecordRepository.findByPaymentIdAndDeletedAtIsNull(paymentId)).thenReturn(Optional.of(record));

        PaymentRecordResponse response = paymentService.getPaymentById(paymentId);

        assertNotNull(response);
        assertEquals(paymentId, response.paymentId());
        assertEquals(BigDecimal.valueOf(500.00), response.amount());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when payment not found")
    void getPaymentById_NotFound() {
        UUID paymentId = UUID.randomUUID();
        when(paymentRecordRepository.findByPaymentIdAndDeletedAtIsNull(paymentId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> paymentService.getPaymentById(paymentId));
    }

    @Test
    @DisplayName("Should list payments filtered by donation ID")
    void findPayments_ByDonationId() {
        UUID donationId = UUID.randomUUID();
        PaymentRecord record = new PaymentRecord();
        record.setPaymentId(UUID.randomUUID());
        record.setDonationId(donationId);

        Pageable pageable = PageRequest.of(0, 10);
        when(paymentRecordRepository.findAllByDonationIdAndDeletedAtIsNull(donationId, pageable))
            .thenReturn(new PageImpl<>(List.of(record), pageable, 1));

        Page<PaymentRecordResponse> page = paymentService.findPayments(donationId, null, pageable);

        assertEquals(1, page.getTotalElements());
        verify(paymentRecordRepository).findAllByDonationIdAndDeletedAtIsNull(donationId, pageable);
    }
}
