package com.disasterreliefnet.verification.service;

import com.disasterreliefnet.common.enums.CaseStatus;
import com.disasterreliefnet.common.event.KafkaTopics;
import com.disasterreliefnet.common.event.payload.RequestVerifiedPayload;
import com.disasterreliefnet.common.outbox.OutboxService;
import com.disasterreliefnet.verification.domain.VerificationCase;
import com.disasterreliefnet.verification.domain.VerificationDocument;
import com.disasterreliefnet.verification.dto.*;
import com.disasterreliefnet.verification.exception.DuplicateCaseException;
import com.disasterreliefnet.verification.exception.InvalidCaseStatusException;
import com.disasterreliefnet.verification.exception.VerificationCaseNotFoundException;
import com.disasterreliefnet.verification.mapper.VerificationMapper;
import com.disasterreliefnet.verification.repository.VerificationCaseRepository;
import com.disasterreliefnet.verification.repository.VerificationDocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Core business service managing verification cases, document uploads, and case reviews.
 */
@Service
public class VerificationService {

    private static final Logger log = LoggerFactory.getLogger(VerificationService.class);

    private final VerificationCaseRepository verificationCaseRepository;
    private final VerificationDocumentRepository verificationDocumentRepository;
    private final VerificationMapper verificationMapper;
    private final OutboxService outboxService;

    public VerificationService(
        VerificationCaseRepository verificationCaseRepository,
        VerificationDocumentRepository verificationDocumentRepository,
        VerificationMapper verificationMapper,
        OutboxService outboxService
    ) {
        this.verificationCaseRepository = verificationCaseRepository;
        this.verificationDocumentRepository = verificationDocumentRepository;
        this.verificationMapper = verificationMapper;
        this.outboxService = outboxService;
    }

    /**
     * Opens a new verification case triggered by RequestCreated event.
     */
    @Transactional
    public VerificationCaseResponse openCase(UUID requestId, UUID receiverId) {
        log.info("Opening verification case: requestId={}, receiverId={}", requestId, receiverId);

        if (verificationCaseRepository.existsByRequestIdAndDeletedAtIsNull(requestId)) {
            log.warn("Verification case already exists for requestId={}", requestId);
            return verificationCaseRepository.findByRequestId(requestId)
                .map(verificationMapper::toCaseResponse)
                .orElseThrow(() -> new DuplicateCaseException("Case already exists for requestId: " + requestId));
        }

        VerificationCase verificationCase = VerificationCase.builder()
            .caseId(UUID.randomUUID())
            .requestId(requestId)
            .receiverId(receiverId)
            .status(CaseStatus.OPEN)
            .openedAt(Instant.now())
            .build();

        VerificationCase saved = verificationCaseRepository.save(verificationCase);
        log.info("Verification case opened successfully: caseId={}", saved.getCaseId());
        return verificationMapper.toCaseResponse(saved);
    }

    /**
     * Retrieves an active verification case by its caseId.
     */
    @Transactional(readOnly = true)
    public VerificationCaseResponse getCase(UUID caseId) {
        return verificationMapper.toCaseResponse(findActiveCase(caseId));
    }

    /**
     * Retrieves an active verification case by the associated requestId.
     */
    @Transactional(readOnly = true)
    public VerificationCaseResponse getCaseByRequestId(UUID requestId) {
        return verificationCaseRepository.findByRequestId(requestId)
            .map(verificationMapper::toCaseResponse)
            .orElseThrow(() -> new VerificationCaseNotFoundException("Verification case not found for requestId: " + requestId));
    }

    /**
     * Lists active verification cases, optionally filtered by status.
     */
    @Transactional(readOnly = true)
    public Page<VerificationCaseResponse> listCases(CaseStatus status, Pageable pageable) {
        Page<VerificationCase> page = (status != null)
            ? verificationCaseRepository.findAllByStatus(status, pageable)
            : verificationCaseRepository.findAllActive(pageable);
        return page.map(verificationMapper::toCaseResponse);
    }

    /**
     * Approves a verification case and publishes RequestVerified event via outbox.
     */
    @Transactional
    public VerificationCaseResponse approveCase(UUID caseId, ApproveVerificationRequest request, String reviewer) {
        log.info("Approving verification case: caseId={}, reviewer={}", caseId, reviewer);
        VerificationCase verificationCase = findActiveCase(caseId);

        if (verificationCase.getStatus() == CaseStatus.APPROVED || verificationCase.getStatus() == CaseStatus.REJECTED) {
            throw new InvalidCaseStatusException("Cannot approve already resolved case in status: " + verificationCase.getStatus());
        }

        verificationCase.setStatus(CaseStatus.APPROVED);
        verificationCase.setReviewedBy(reviewer);
        verificationCase.setReviewNotes(request.reviewNotes());
        verificationCase.setResolvedAt(Instant.now());

        VerificationCase saved = verificationCaseRepository.save(verificationCase);

        // Enqueue RequestVerified event in outbox
        RequestVerifiedPayload payload = new RequestVerifiedPayload(
            saved.getRequestId(),
            saved.getReceiverId(),
            saved.getCaseId(),
            "APPROVED",
            reviewer,
            request.reviewNotes()
        );

        outboxService.enqueue(
            KafkaTopics.REQUEST_VERIFIED,
            "VERIFICATION_CASE",
            saved.getCaseId(),
            "RequestVerified",
            payload,
            saved.getRequestId().toString()
        );

        log.info("Verification case approved and RequestVerified event enqueued: caseId={}", saved.getCaseId());
        return verificationMapper.toCaseResponse(saved);
    }

    /**
     * Rejects a verification case and publishes RequestVerified event via outbox.
     */
    @Transactional
    public VerificationCaseResponse rejectCase(UUID caseId, RejectVerificationRequest request, String reviewer) {
        log.info("Rejecting verification case: caseId={}, reviewer={}", caseId, reviewer);
        VerificationCase verificationCase = findActiveCase(caseId);

        if (verificationCase.getStatus() == CaseStatus.APPROVED || verificationCase.getStatus() == CaseStatus.REJECTED) {
            throw new InvalidCaseStatusException("Cannot reject already resolved case in status: " + verificationCase.getStatus());
        }

        verificationCase.setStatus(CaseStatus.REJECTED);
        verificationCase.setReviewedBy(reviewer);
        verificationCase.setReviewNotes(request.reviewNotes());
        verificationCase.setResolvedAt(Instant.now());

        VerificationCase saved = verificationCaseRepository.save(verificationCase);

        // Enqueue RequestVerified event in outbox
        RequestVerifiedPayload payload = new RequestVerifiedPayload(
            saved.getRequestId(),
            saved.getReceiverId(),
            saved.getCaseId(),
            "REJECTED",
            reviewer,
            request.reviewNotes()
        );

        outboxService.enqueue(
            KafkaTopics.REQUEST_VERIFIED,
            "VERIFICATION_CASE",
            saved.getCaseId(),
            "RequestVerified",
            payload,
            saved.getRequestId().toString()
        );

        log.info("Verification case rejected and RequestVerified event enqueued: caseId={}", saved.getCaseId());
        return verificationMapper.toCaseResponse(saved);
    }

    /**
     * Uploads a supporting document for an active case and advances OPEN -> UNDER_REVIEW.
     */
    @Transactional
    public VerificationCaseResponse uploadDocument(UUID caseId, UploadDocumentRequest request) {
        log.info("Uploading document for case: caseId={}, docType={}", caseId, request.documentType());
        VerificationCase verificationCase = findActiveCase(caseId);

        if (verificationCase.getStatus() == CaseStatus.APPROVED || verificationCase.getStatus() == CaseStatus.REJECTED) {
            throw new InvalidCaseStatusException("Cannot upload documents for already resolved case in status: " + verificationCase.getStatus());
        }

        VerificationDocument document = VerificationDocument.builder()
            .documentId(UUID.randomUUID())
            .verificationCase(verificationCase)
            .documentType(request.documentType())
            .fileUrl(request.fileUrl())
            .uploadedAt(Instant.now())
            .build();

        verificationCase.addDocument(document);

        // Transition from OPEN to UNDER_REVIEW on document upload
        if (verificationCase.getStatus() == CaseStatus.OPEN) {
            verificationCase.setStatus(CaseStatus.UNDER_REVIEW);
        }

        VerificationCase saved = verificationCaseRepository.save(verificationCase);
        return verificationMapper.toCaseResponse(saved);
    }

    private VerificationCase findActiveCase(UUID caseId) {
        return verificationCaseRepository.findActiveById(caseId)
            .orElseThrow(() -> new VerificationCaseNotFoundException(caseId));
    }
}
