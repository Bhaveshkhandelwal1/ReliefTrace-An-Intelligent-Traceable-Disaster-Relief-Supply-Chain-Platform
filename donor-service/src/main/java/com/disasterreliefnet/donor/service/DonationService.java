package com.disasterreliefnet.donor.service;

import com.disasterreliefnet.common.enums.DonationStatus;
import com.disasterreliefnet.common.enums.DonationType;
import com.disasterreliefnet.common.enums.DonorStatus;
import com.disasterreliefnet.common.event.KafkaTopics;
import com.disasterreliefnet.common.event.payload.DonationRegisteredPayload;
import com.disasterreliefnet.common.exception.BusinessRuleException;
import com.disasterreliefnet.common.exception.ResourceNotFoundException;
import com.disasterreliefnet.common.outbox.OutboxService;
import com.disasterreliefnet.donor.domain.Donation;
import com.disasterreliefnet.donor.domain.DonationItem;
import com.disasterreliefnet.donor.domain.Donor;
import com.disasterreliefnet.donor.dto.*;
import com.disasterreliefnet.donor.mapper.DonationMapper;
import com.disasterreliefnet.donor.repository.DonationRepository;
import com.disasterreliefnet.donor.repository.DonorRepository;
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
 * Service managing donation pledges, in-kind line items, receipts, and event publishing.
 */
@Service
@Transactional
public class DonationService {

    private static final Logger log = LoggerFactory.getLogger(DonationService.class);

    private final DonationRepository donationRepository;
    private final DonorRepository donorRepository;
    private final DonationMapper donationMapper;
    private final OutboxService outboxService;

    public DonationService(
        DonationRepository donationRepository,
        DonorRepository donorRepository,
        DonationMapper donationMapper,
        OutboxService outboxService
    ) {
        this.donationRepository = donationRepository;
        this.donorRepository = donorRepository;
        this.donationMapper = donationMapper;
        this.outboxService = outboxService;
    }

    /**
     * Creates a new monetary donation or in-kind pledge, storing items and enqueueing DonationRegistered event.
     */
    public DonationResponse createDonation(UUID donorId, CreateDonationRequest request) {
        log.info("Creating donation for donor: donorId={}, type={}", donorId, request.donationType());

        Donor donor = donorRepository.findByDonorIdAndDeletedAtIsNull(donorId)
            .orElseThrow(() -> new ResourceNotFoundException("Donor not found with id: " + donorId));

        if (donor.getStatus() != DonorStatus.ACTIVE) {
            throw new BusinessRuleException("Cannot pledge donations for inactive or suspended donor: " + donorId);
        }

        // Validate business constraints per donation type
        if (request.donationType() == DonationType.MONETARY) {
            if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessRuleException("Monetary donation must have an amount greater than zero");
            }
            if (request.currency() == null || request.currency().isBlank()) {
                throw new BusinessRuleException("Monetary donation must specify a 3-letter currency code (e.g. USD, EUR)");
            }
        } else if (request.donationType() == DonationType.IN_KIND) {
            boolean hasDescription = request.itemDescription() != null && !request.itemDescription().isBlank();
            boolean hasItems = request.items() != null && !request.items().isEmpty();
            if (!hasDescription && !hasItems) {
                throw new BusinessRuleException("In-kind donation must provide either itemDescription or item lines");
            }
        }

        Donation donation = donationMapper.toEntity(request, donor);

        // Add line items if present
        if (request.items() != null && !request.items().isEmpty()) {
            for (DonationItemDto itemDto : request.items()) {
                DonationItem item = donationMapper.toItemEntity(itemDto);
                donation.addItem(item);
            }
        }

        Donation saved = donationRepository.save(donation);

        // Transactional outbox event
        String donorName = donor.getFirstName() + " " + donor.getLastName();
        DonationRegisteredPayload payload = new DonationRegisteredPayload(
            saved.getDonationId(),
            donor.getDonorId(),
            saved.getDonationType(),
            saved.getAmount(),
            saved.getCurrency(),
            saved.getItemDescription(),
            saved.getQuantityKg(),
            saved.getDisasterId(),
            donor.getEmail(),
            donorName
        );

        outboxService.enqueue(
            KafkaTopics.DONATION_REGISTERED,
            "DONATION",
            saved.getDonationId(),
            "DonationRegistered",
            payload,
            null
        );

        log.info("Donation registered successfully: donationId={}, status={}", saved.getDonationId(), saved.getStatus());
        return donationMapper.toResponse(saved);
    }

    /**
     * Retrieves paged donation history for a donor.
     */
    @Transactional(readOnly = true)
    public Page<DonationResponse> getDonationsByDonor(UUID donorId, Pageable pageable) {
        if (!donorRepository.existsById(donorId)) {
            throw new ResourceNotFoundException("Donor not found with id: " + donorId);
        }
        return donationRepository.findByDonor_DonorIdAndDeletedAtIsNull(donorId, pageable)
            .map(donationMapper::toResponse);
    }

    /**
     * Retrieves specific donation details.
     */
    @Transactional(readOnly = true)
    public DonationResponse getDonationById(UUID donorId, UUID donationId) {
        Donation donation = donationRepository.findByDonationIdAndDonor_DonorIdAndDeletedAtIsNull(donationId, donorId)
            .orElseThrow(() -> new ResourceNotFoundException("Donation not found with id: " + donationId + " for donor: " + donorId));
        return donationMapper.toResponse(donation);
    }

    /**
     * Cancels an unfulfilled donation pledge.
     */
    public DonationResponse cancelDonation(UUID donorId, UUID donationId) {
        log.info("Cancelling donation: donationId={}, donorId={}", donationId, donorId);

        Donation donation = donationRepository.findByDonationIdAndDonor_DonorIdAndDeletedAtIsNull(donationId, donorId)
            .orElseThrow(() -> new ResourceNotFoundException("Donation not found with id: " + donationId));

        if (donation.getStatus() == DonationStatus.PAID || donation.getStatus() == DonationStatus.RECEIVED) {
            throw new BusinessRuleException("Cannot cancel a completed or fulfilled donation: " + donationId);
        }

        if (donation.getStatus() == DonationStatus.CANCELLED) {
            throw new BusinessRuleException("Donation is already cancelled: " + donationId);
        }

        donation.setStatus(DonationStatus.CANCELLED);
        Donation updated = donationRepository.save(donation);
        return donationMapper.toResponse(updated);
    }

    /**
     * Transitions donation lifecycle status (e.g. PLEDGED -> RECEIVED, PENDING -> PAID, or REJECTED).
     */
    public DonationResponse transitionDonationStatus(UUID donationId, DonationStatus newStatus, String notes) {
        log.info("Transitioning donation status: donationId={}, newStatus={}", donationId, newStatus);

        Donation donation = donationRepository.findByDonationIdAndDeletedAtIsNull(donationId)
            .orElseThrow(() -> new ResourceNotFoundException("Donation not found with id: " + donationId));

        if (notes != null && !notes.isBlank()) {
            donation.setNotes(donation.getNotes() != null ? donation.getNotes() + " | " + notes : notes);
        }

        donation.setStatus(newStatus);

        // Auto-generate receipt and accumulate total donation amount upon fulfillment
        if (newStatus == DonationStatus.PAID || newStatus == DonationStatus.RECEIVED) {
            if (donation.getReceiptNumber() == null) {
                donation.generateReceipt("REC");
            }
            if (donation.getAmount() != null) {
                donation.getDonor().recordContribution(donation.getAmount());
                donorRepository.save(donation.getDonor());
            }
        }

        Donation saved = donationRepository.save(donation);
        return donationMapper.toResponse(saved);
    }

    /**
     * Generates or fetches an official tax/audit receipt for a donation.
     */
    public DonationReceiptDto getDonationReceipt(UUID donorId, UUID donationId) {
        Donation donation = donationRepository.findByDonationIdAndDonor_DonorIdAndDeletedAtIsNull(donationId, donorId)
            .orElseThrow(() -> new ResourceNotFoundException("Donation not found with id: " + donationId));

        if (donation.getStatus() != DonationStatus.PAID &&
            donation.getStatus() != DonationStatus.RECEIVED &&
            donation.getStatus() != DonationStatus.CONFIRMED) {
            throw new BusinessRuleException("Receipts can only be generated for confirmed or fulfilled donations. Current status: " + donation.getStatus());
        }

        if (donation.getReceiptNumber() == null) {
            donation.generateReceipt("REC");
            donationRepository.save(donation);
        }

        Donor donor = donation.getDonor();
        String donorName = donor.getFirstName() + " " + donor.getLastName();

        return new DonationReceiptDto(
            donation.getReceiptNumber(),
            donation.getDonationId(),
            donor.getDonorId(),
            donorName,
            donor.getEmail(),
            donation.getDonationType(),
            donation.getAmount(),
            donation.getCurrency(),
            donation.getItemDescription(),
            donation.getReceiptIssuedAt() != null ? donation.getReceiptIssuedAt() : Instant.now(),
            true
        );
    }
}
