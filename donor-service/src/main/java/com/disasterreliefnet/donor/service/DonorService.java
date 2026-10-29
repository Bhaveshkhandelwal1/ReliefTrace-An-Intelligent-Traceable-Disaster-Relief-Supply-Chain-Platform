package com.disasterreliefnet.donor.service;

import com.disasterreliefnet.common.enums.DonorStatus;
import com.disasterreliefnet.common.exception.ConflictException;
import com.disasterreliefnet.common.exception.ResourceNotFoundException;
import com.disasterreliefnet.donor.client.VerificationClient;
import com.disasterreliefnet.donor.client.dto.VerificationStatusDto;
import com.disasterreliefnet.donor.domain.Donor;
import com.disasterreliefnet.donor.dto.*;
import com.disasterreliefnet.donor.mapper.DonorMapper;
import com.disasterreliefnet.donor.repository.DonationRepository;
import com.disasterreliefnet.donor.repository.DonorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Service managing donor lifecycle, profiles, and impact tracking.
 */
@Service
@Transactional
public class DonorService {

    private static final Logger log = LoggerFactory.getLogger(DonorService.class);

    private final DonorRepository donorRepository;
    private final DonationRepository donationRepository;
    private final DonorMapper donorMapper;
    private final VerificationClient verificationClient;

    public DonorService(
        DonorRepository donorRepository,
        DonationRepository donationRepository,
        DonorMapper donorMapper,
        VerificationClient verificationClient
    ) {
        this.donorRepository = donorRepository;
        this.donationRepository = donationRepository;
        this.donorMapper = donorMapper;
        this.verificationClient = verificationClient;
    }

    /**
     * Registers a new donor profile.
     */
    public DonorResponse createDonor(CreateDonorRequest request) {
        log.info("Registering new donor: email={}, type={}", request.email(), request.donorType());

        if (donorRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull(request.email())) {
            throw new ConflictException("Email is already registered: " + request.email());
        }

        Donor donor = donorMapper.toEntity(request);
        Donor saved = donorRepository.save(donor);

        log.info("Donor registered successfully: donorId={}", saved.getDonorId());
        return donorMapper.toResponse(saved);
    }

    /**
     * Retrieves donor profile by ID.
     */
    @Transactional(readOnly = true)
    public DonorResponse getDonorById(UUID donorId) {
        Donor donor = donorRepository.findByDonorIdAndDeletedAtIsNull(donorId)
            .orElseThrow(() -> new ResourceNotFoundException("Donor not found with id: " + donorId));
        return donorMapper.toResponse(donor);
    }

    /**
     * Paged retrieval of all active donors.
     */
    @Transactional(readOnly = true)
    public Page<DonorResponse> getDonors(Pageable pageable) {
        return donorRepository.findByDeletedAtIsNull(pageable)
            .map(donorMapper::toResponse);
    }

    /**
     * Updates profile information for an existing donor.
     */
    public DonorResponse updateDonor(UUID donorId, UpdateDonorRequest request) {
        log.info("Updating donor profile: donorId={}", donorId);

        Donor donor = donorRepository.findByDonorIdAndDeletedAtIsNull(donorId)
            .orElseThrow(() -> new ResourceNotFoundException("Donor not found with id: " + donorId));

        donorMapper.updateEntityFromDto(request, donor);
        Donor updated = donorRepository.save(donor);

        return donorMapper.toResponse(updated);
    }

    /**
     * Soft-deletes a donor account.
     */
    public void deleteDonor(UUID donorId) {
        log.info("Deactivating donor account: donorId={}", donorId);

        Donor donor = donorRepository.findByDonorIdAndDeletedAtIsNull(donorId)
            .orElseThrow(() -> new ResourceNotFoundException("Donor not found with id: " + donorId));

        donor.setStatus(DonorStatus.DELETED);
        donor.setDeletedAt(Instant.now());
        donorRepository.save(donor);
    }

    /**
     * Computes the aggregated relief impact for a donor.
     */
    @Transactional(readOnly = true)
    public DonorImpactSummaryDto getDonorImpactSummary(UUID donorId) {
        Donor donor = donorRepository.findByDonorIdAndDeletedAtIsNull(donorId)
            .orElseThrow(() -> new ResourceNotFoundException("Donor not found with id: " + donorId));

        long donationCount = donationRepository.countByDonor_DonorIdAndDeletedAtIsNull(donorId);
        String fullName = donor.getFirstName() + " " + donor.getLastName();

        return new DonorImpactSummaryDto(
            donor.getDonorId(),
            fullName,
            donor.getDonorType(),
            donationCount,
            donor.getTotalDonatedAmount(),
            donor.getImpactDeliveriesCount(),
            donor.getCreatedAt()
        );
    }

    /**
     * Synchronously checks donor verification status with the Verification Service via Feign.
     */
    @Transactional(readOnly = true)
    public VerificationStatusDto checkDonorVerification(UUID donorId) {
        log.info("Checking verification status for donor: donorId={}", donorId);
        if (!donorRepository.existsById(donorId)) {
            throw new ResourceNotFoundException("Donor not found with id: " + donorId);
        }
        return verificationClient.getDonorVerificationStatus(donorId).getBody();
    }
}
