package com.disasterreliefnet.donor.service;

import com.disasterreliefnet.common.enums.DonationStatus;
import com.disasterreliefnet.common.enums.DonationType;
import com.disasterreliefnet.common.enums.DonorStatus;
import com.disasterreliefnet.common.enums.DonorType;
import com.disasterreliefnet.common.event.KafkaTopics;
import com.disasterreliefnet.common.exception.BusinessRuleException;
import com.disasterreliefnet.common.exception.ResourceNotFoundException;
import com.disasterreliefnet.common.outbox.OutboxService;
import com.disasterreliefnet.donor.domain.Donation;
import com.disasterreliefnet.donor.domain.Donor;
import com.disasterreliefnet.donor.dto.*;
import com.disasterreliefnet.donor.mapper.DonationMapper;
import com.disasterreliefnet.donor.mapper.DonationMapperImpl;
import com.disasterreliefnet.donor.repository.DonationRepository;
import com.disasterreliefnet.donor.repository.DonorRepository;
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
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DonationServiceTest {

    @Mock
    private DonationRepository donationRepository;

    @Mock
    private DonorRepository donorRepository;

    @Mock
    private OutboxService outboxService;

    private final DonationMapper donationMapper = new DonationMapperImpl();
    private DonationService donationService;

    @BeforeEach
    void setUp() {
        donationService = new DonationService(
            donationRepository,
            donorRepository,
            donationMapper,
            outboxService
        );
    }

    @Test
    @DisplayName("Should create monetary donation and publish DonationRegistered outbox event")
    void shouldCreateMonetaryDonationSuccessfully() {
        UUID donorId = UUID.randomUUID();
        Donor donor = Donor.builder()
            .donorId(donorId)
            .firstName("Alice")
            .lastName("Wong")
            .email("alice@example.com")
            .status(DonorStatus.ACTIVE)
            .donorType(DonorType.INDIVIDUAL)
            .build();

        CreateDonationRequest request = new CreateDonationRequest(
            DonationType.MONETARY,
            new BigDecimal("500.00"),
            "USD",
            null,
            null,
            UUID.randomUUID(),
            "Earmarked for flood relief",
            null
        );

        when(donorRepository.findByDonorIdAndDeletedAtIsNull(donorId)).thenReturn(Optional.of(donor));
        when(donationRepository.save(any(Donation.class))).thenAnswer(invocation -> {
            Donation d = invocation.getArgument(0);
            d.setDonationId(UUID.randomUUID());
            d.setStatus(DonationStatus.PENDING);
            d.setCreatedAt(Instant.now());
            d.setUpdatedAt(Instant.now());
            return d;
        });

        DonationResponse response = donationService.createDonation(donorId, request);

        assertNotNull(response);
        assertNotNull(response.donationId());
        assertEquals(donorId, response.donorId());
        assertEquals(DonationType.MONETARY, response.donationType());
        assertEquals(new BigDecimal("500.00"), response.amount());
        assertEquals("USD", response.currency());
        assertEquals(DonationStatus.PENDING, response.status());

        verify(outboxService).enqueue(
            eq(KafkaTopics.DONATION_REGISTERED),
            eq("DONATION"),
            eq(response.donationId()),
            eq("DonationRegistered"),
            any(),
            isNull()
        );
    }

    @Test
    @DisplayName("Should create in-kind donation pledge with item lines")
    void shouldCreateInKindDonationWithItems() {
        UUID donorId = UUID.randomUUID();
        Donor donor = Donor.builder()
            .donorId(donorId)
            .firstName("HealthAid")
            .lastName("NGO")
            .email("contact@healthaid.org")
            .status(DonorStatus.ACTIVE)
            .donorType(DonorType.NGO)
            .build();

        List<DonationItemDto> items = List.of(
            new DonationItemDto(null, "Antibiotics Pack", "MEDICINE", new BigDecimal("100"), "boxes", new BigDecimal("2500.00")),
            new DonationItemDto(null, "Bandages Roll", "MEDICINE", new BigDecimal("500"), "units", new BigDecimal("500.00"))
        );

        CreateDonationRequest request = new CreateDonationRequest(
            DonationType.IN_KIND,
            null,
            null,
            "Emergency medical supplies kit",
            new BigDecimal("85.50"),
            null,
            "First aid response supplies",
            items
        );

        when(donorRepository.findByDonorIdAndDeletedAtIsNull(donorId)).thenReturn(Optional.of(donor));
        when(donationRepository.save(any(Donation.class))).thenAnswer(invocation -> {
            Donation d = invocation.getArgument(0);
            d.setDonationId(UUID.randomUUID());
            d.setStatus(DonationStatus.PLEDGED);
            d.setCreatedAt(Instant.now());
            d.setUpdatedAt(Instant.now());
            return d;
        });

        DonationResponse response = donationService.createDonation(donorId, request);

        assertNotNull(response);
        assertEquals(DonationType.IN_KIND, response.donationType());
        assertEquals(DonationStatus.PLEDGED, response.status());
        assertEquals(2, response.items().size());
        assertEquals("Antibiotics Pack", response.items().get(0).itemName());

        verify(outboxService).enqueue(
            eq(KafkaTopics.DONATION_REGISTERED),
            eq("DONATION"),
            any(),
            eq("DonationRegistered"),
            any(),
            isNull()
        );
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when monetary donation lacks amount or currency")
    void shouldRejectInvalidMonetaryDonation() {
        UUID donorId = UUID.randomUUID();
        Donor donor = Donor.builder().donorId(donorId).status(DonorStatus.ACTIVE).build();
        when(donorRepository.findByDonorIdAndDeletedAtIsNull(donorId)).thenReturn(Optional.of(donor));

        CreateDonationRequest request = new CreateDonationRequest(
            DonationType.MONETARY,
            null, // missing amount
            "USD",
            null, null, null, null, null
        );

        assertThrows(BusinessRuleException.class, () -> donationService.createDonation(donorId, request));
    }

    @Test
    @DisplayName("Should cancel donation pledge when not yet fulfilled")
    void shouldCancelDonationPledge() {
        UUID donorId = UUID.randomUUID();
        UUID donationId = UUID.randomUUID();
        Donation donation = Donation.builder()
            .donationId(donationId)
            .status(DonationStatus.PLEDGED)
            .build();

        when(donationRepository.findByDonationIdAndDonor_DonorIdAndDeletedAtIsNull(donationId, donorId))
            .thenReturn(Optional.of(donation));
        when(donationRepository.save(any(Donation.class))).thenAnswer(i -> i.getArgument(0));

        DonationResponse response = donationService.cancelDonation(donorId, donationId);

        assertNotNull(response);
        assertEquals(DonationStatus.CANCELLED, response.status());
    }

    @Test
    @DisplayName("Should reject cancellation of already paid/received donation")
    void shouldRejectCancellationOfPaidDonation() {
        UUID donorId = UUID.randomUUID();
        UUID donationId = UUID.randomUUID();
        Donation donation = Donation.builder()
            .donationId(donationId)
            .status(DonationStatus.PAID)
            .build();

        when(donationRepository.findByDonationIdAndDonor_DonorIdAndDeletedAtIsNull(donationId, donorId))
            .thenReturn(Optional.of(donation));

        assertThrows(BusinessRuleException.class, () -> donationService.cancelDonation(donorId, donationId));
    }

    @Test
    @DisplayName("Should transition donation status and generate receipt on fulfillment")
    void shouldTransitionStatusAndGenerateReceipt() {
        UUID donorId = UUID.randomUUID();
        UUID donationId = UUID.randomUUID();
        Donor donor = Donor.builder()
            .donorId(donorId)
            .firstName("Sam")
            .lastName("Alt")
            .totalDonatedAmount(BigDecimal.ZERO)
            .build();

        Donation donation = Donation.builder()
            .donationId(donationId)
            .donor(donor)
            .donationType(DonationType.MONETARY)
            .amount(new BigDecimal("1000.00"))
            .currency("USD")
            .status(DonationStatus.PENDING)
            .build();

        when(donationRepository.findByDonationIdAndDeletedAtIsNull(donationId)).thenReturn(Optional.of(donation));
        when(donationRepository.save(any(Donation.class))).thenAnswer(i -> i.getArgument(0));

        DonationResponse response = donationService.transitionDonationStatus(donationId, DonationStatus.PAID, "Payment verified");

        assertNotNull(response);
        assertEquals(DonationStatus.PAID, response.status());
        assertNotNull(response.receiptNumber());
        assertTrue(response.receiptNumber().startsWith("REC-"));
        assertEquals(new BigDecimal("1000.00"), donor.getTotalDonatedAmount());
        verify(donorRepository).save(donor);
    }

    @Test
    @DisplayName("Should generate or fetch tax receipt for confirmed/paid donation")
    void shouldGetDonationReceipt() {
        UUID donorId = UUID.randomUUID();
        UUID donationId = UUID.randomUUID();
        Donor donor = Donor.builder()
            .donorId(donorId)
            .firstName("Linda")
            .lastName("Taylor")
            .email("linda@example.com")
            .build();

        Donation donation = Donation.builder()
            .donationId(donationId)
            .donor(donor)
            .donationType(DonationType.MONETARY)
            .amount(new BigDecimal("250.00"))
            .currency("USD")
            .status(DonationStatus.PAID)
            .receiptNumber("REC-1234ABCD")
            .receiptIssuedAt(Instant.now())
            .build();

        when(donationRepository.findByDonationIdAndDonor_DonorIdAndDeletedAtIsNull(donationId, donorId))
            .thenReturn(Optional.of(donation));

        DonationReceiptDto receipt = donationService.getDonationReceipt(donorId, donationId);

        assertNotNull(receipt);
        assertEquals("REC-1234ABCD", receipt.receiptNumber());
        assertEquals("Linda Taylor", receipt.donorName());
        assertEquals(new BigDecimal("250.00"), receipt.amount());
        assertTrue(receipt.taxDeductible());
    }
}
