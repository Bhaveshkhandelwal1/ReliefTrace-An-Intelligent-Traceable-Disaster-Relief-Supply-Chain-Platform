package com.disasterreliefnet.donor.service;

import com.disasterreliefnet.common.enums.DonorStatus;
import com.disasterreliefnet.common.enums.DonorType;
import com.disasterreliefnet.common.enums.VerificationStatus;
import com.disasterreliefnet.common.exception.ConflictException;
import com.disasterreliefnet.common.exception.ResourceNotFoundException;
import com.disasterreliefnet.donor.client.VerificationClient;
import com.disasterreliefnet.donor.client.dto.VerificationStatusDto;
import com.disasterreliefnet.donor.domain.Donor;
import com.disasterreliefnet.donor.dto.*;
import com.disasterreliefnet.donor.mapper.DonorMapper;
import com.disasterreliefnet.donor.mapper.DonorMapperImpl;
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
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DonorServiceTest {

    @Mock
    private DonorRepository donorRepository;

    @Mock
    private DonationRepository donationRepository;

    @Mock
    private VerificationClient verificationClient;

    private final DonorMapper donorMapper = new DonorMapperImpl();
    private DonorService donorService;

    @BeforeEach
    void setUp() {
        donorService = new DonorService(
            donorRepository,
            donationRepository,
            donorMapper,
            verificationClient
        );
    }

    @Test
    @DisplayName("Should create donor successfully when email is unique")
    void shouldCreateDonorSuccessfully() {
        CreateDonorRequest request = new CreateDonorRequest(
            "Acme",
            "Pharma Corp",
            "aid@acme.com",
            "+14155552671",
            DonorType.PHARMACY,
            new AddressDto("100 Biotech Way", "San Francisco", "CA", "US", "94107"),
            "US-TAX-998877"
        );

        when(donorRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull("aid@acme.com")).thenReturn(false);
        when(donorRepository.save(any(Donor.class))).thenAnswer(invocation -> {
            Donor d = invocation.getArgument(0);
            d.setDonorId(UUID.randomUUID());
            d.setCreatedAt(Instant.now());
            d.setUpdatedAt(Instant.now());
            return d;
        });

        DonorResponse response = donorService.createDonor(request);

        assertNotNull(response);
        assertNotNull(response.donorId());
        assertEquals("aid@acme.com", response.email());
        assertEquals(DonorType.PHARMACY, response.donorType());
        assertEquals(DonorStatus.ACTIVE, response.status());
        assertEquals("San Francisco", response.address().city());
    }

    @Test
    @DisplayName("Should throw ConflictException when creating donor with already registered email")
    void shouldThrowConflictWhenEmailExists() {
        CreateDonorRequest request = new CreateDonorRequest(
            "John",
            "Doe",
            "john@example.com",
            null,
            DonorType.INDIVIDUAL,
            null,
            null
        );

        when(donorRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull("john@example.com")).thenReturn(true);

        assertThrows(ConflictException.class, () -> donorService.createDonor(request));
        verify(donorRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should get donor by ID")
    void shouldGetDonorById() {
        UUID donorId = UUID.randomUUID();
        Donor donor = Donor.builder()
            .donorId(donorId)
            .firstName("Red")
            .lastName("Cross Chapter")
            .email("relief@ngo.org")
            .donorType(DonorType.NGO)
            .status(DonorStatus.ACTIVE)
            .totalDonatedAmount(new BigDecimal("50000.00"))
            .impactDeliveriesCount(12L)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        when(donorRepository.findByDonorIdAndDeletedAtIsNull(donorId)).thenReturn(Optional.of(donor));

        DonorResponse response = donorService.getDonorById(donorId);

        assertNotNull(response);
        assertEquals(donorId, response.donorId());
        assertEquals("relief@ngo.org", response.email());
        assertEquals(DonorType.NGO, response.donorType());
        assertEquals(new BigDecimal("50000.00"), response.totalDonatedAmount());
        assertEquals(12L, response.impactDeliveriesCount());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when donor does not exist")
    void shouldThrowNotFoundWhenDonorMissing() {
        UUID donorId = UUID.randomUUID();
        when(donorRepository.findByDonorIdAndDeletedAtIsNull(donorId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> donorService.getDonorById(donorId));
    }

    @Test
    @DisplayName("Should list donors with pagination")
    void shouldListDonorsPaged() {
        Pageable pageable = PageRequest.of(0, 10);
        Donor donor = Donor.builder()
            .donorId(UUID.randomUUID())
            .firstName("Jane")
            .lastName("Smith")
            .email("jane@corp.org")
            .donorType(DonorType.CORPORATE)
            .status(DonorStatus.ACTIVE)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        when(donorRepository.findByDeletedAtIsNull(pageable)).thenReturn(new PageImpl<>(List.of(donor)));

        Page<DonorResponse> result = donorService.getDonors(pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("jane@corp.org", result.getContent().get(0).email());
    }

    @Test
    @DisplayName("Should update donor profile fields")
    void shouldUpdateDonorSuccessfully() {
        UUID donorId = UUID.randomUUID();
        Donor donor = Donor.builder()
            .donorId(donorId)
            .firstName("OldFirst")
            .lastName("OldLast")
            .email("donor@example.com")
            .phone("+1234567890")
            .donorType(DonorType.INDIVIDUAL)
            .status(DonorStatus.ACTIVE)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        when(donorRepository.findByDonorIdAndDeletedAtIsNull(donorId)).thenReturn(Optional.of(donor));
        when(donorRepository.save(any(Donor.class))).thenAnswer(i -> i.getArgument(0));

        UpdateDonorRequest updateRequest = new UpdateDonorRequest(
            "NewFirst",
            "NewLast",
            "+9876543210",
            DonorType.INDIVIDUAL,
            new AddressDto("50 Main St", "Boston", "MA", "US", "02101"),
            "TAX-123"
        );

        DonorResponse response = donorService.updateDonor(donorId, updateRequest);

        assertNotNull(response);
        assertEquals("NewFirst", response.firstName());
        assertEquals("NewLast", response.lastName());
        assertEquals("+9876543210", response.phone());
        assertEquals("Boston", response.address().city());
    }

    @Test
    @DisplayName("Should soft-delete donor account")
    void shouldSoftDeleteDonor() {
        UUID donorId = UUID.randomUUID();
        Donor donor = Donor.builder()
            .donorId(donorId)
            .status(DonorStatus.ACTIVE)
            .build();

        when(donorRepository.findByDonorIdAndDeletedAtIsNull(donorId)).thenReturn(Optional.of(donor));

        donorService.deleteDonor(donorId);

        assertEquals(DonorStatus.DELETED, donor.getStatus());
        assertNotNull(donor.getDeletedAt());
        verify(donorRepository).save(donor);
    }

    @Test
    @DisplayName("Should compute donor impact summary")
    void shouldComputeDonorImpactSummary() {
        UUID donorId = UUID.randomUUID();
        Instant joinedAt = Instant.now();
        Donor donor = Donor.builder()
            .donorId(donorId)
            .firstName("Global")
            .lastName("Relief Partners")
            .donorType(DonorType.CORPORATE)
            .totalDonatedAmount(new BigDecimal("125000.00"))
            .impactDeliveriesCount(45L)
            .createdAt(joinedAt)
            .build();

        when(donorRepository.findByDonorIdAndDeletedAtIsNull(donorId)).thenReturn(Optional.of(donor));
        when(donationRepository.countByDonor_DonorIdAndDeletedAtIsNull(donorId)).thenReturn(8L);

        DonorImpactSummaryDto summary = donorService.getDonorImpactSummary(donorId);

        assertNotNull(summary);
        assertEquals(donorId, summary.donorId());
        assertEquals("Global Relief Partners", summary.donorName());
        assertEquals(DonorType.CORPORATE, summary.donorType());
        assertEquals(8L, summary.totalDonationsCount());
        assertEquals(new BigDecimal("125000.00"), summary.totalMonetaryAmount());
        assertEquals(45L, summary.impactDeliveriesCount());
    }

    @Test
    @DisplayName("Should check donor status via Verification Feign client")
    void shouldCheckVerificationStatus() {
        UUID donorId = UUID.randomUUID();
        when(donorRepository.existsById(donorId)).thenReturn(true);
        when(verificationClient.getDonorVerificationStatus(donorId)).thenReturn(
            ResponseEntity.ok(new VerificationStatusDto(donorId, VerificationStatus.VERIFIED, "Verified corporate tax entity"))
        );

        VerificationStatusDto status = donorService.checkDonorVerification(donorId);

        assertNotNull(status);
        assertEquals(VerificationStatus.VERIFIED, status.status());
        assertEquals("Verified corporate tax entity", status.notes());
    }
}
