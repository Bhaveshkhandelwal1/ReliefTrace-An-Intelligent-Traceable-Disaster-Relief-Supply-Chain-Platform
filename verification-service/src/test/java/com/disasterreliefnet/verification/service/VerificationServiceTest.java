package com.disasterreliefnet.verification.service;

import com.disasterreliefnet.common.enums.CaseStatus;
import com.disasterreliefnet.common.enums.DocumentType;
import com.disasterreliefnet.common.event.KafkaTopics;
import com.disasterreliefnet.common.outbox.OutboxService;
import com.disasterreliefnet.verification.domain.VerificationCase;
import com.disasterreliefnet.verification.dto.ApproveVerificationRequest;
import com.disasterreliefnet.verification.dto.RejectVerificationRequest;
import com.disasterreliefnet.verification.dto.UploadDocumentRequest;
import com.disasterreliefnet.verification.dto.VerificationCaseResponse;
import com.disasterreliefnet.verification.exception.InvalidCaseStatusException;
import com.disasterreliefnet.verification.exception.VerificationCaseNotFoundException;
import com.disasterreliefnet.verification.mapper.VerificationMapper;
import com.disasterreliefnet.verification.mapper.VerificationMapperImpl;
import com.disasterreliefnet.verification.repository.VerificationCaseRepository;
import com.disasterreliefnet.verification.repository.VerificationDocumentRepository;
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

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerificationServiceTest {

    @Mock
    private VerificationCaseRepository verificationCaseRepository;

    @Mock
    private VerificationDocumentRepository verificationDocumentRepository;

    @Mock
    private OutboxService outboxService;

    private final VerificationMapper verificationMapper = new VerificationMapperImpl();

    private VerificationService verificationService;

    private UUID caseId;
    private UUID requestId;
    private UUID receiverId;
    private VerificationCase sampleCase;

    @BeforeEach
    void setUp() {
        verificationService = new VerificationService(
            verificationCaseRepository,
            verificationDocumentRepository,
            verificationMapper,
            outboxService
        );

        caseId = UUID.randomUUID();
        requestId = UUID.randomUUID();
        receiverId = UUID.randomUUID();

        sampleCase = VerificationCase.builder()
            .caseId(caseId)
            .requestId(requestId)
            .receiverId(receiverId)
            .status(CaseStatus.OPEN)
            .openedAt(Instant.now())
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    }

    @Test
    @DisplayName("openCase should create a new verification case in OPEN status")
    void shouldOpenCaseSuccessfully() {
        when(verificationCaseRepository.existsByRequestIdAndDeletedAtIsNull(requestId)).thenReturn(false);
        when(verificationCaseRepository.save(any(VerificationCase.class))).thenAnswer(inv -> inv.getArgument(0));

        VerificationCaseResponse response = verificationService.openCase(requestId, receiverId);

        assertNotNull(response);
        assertEquals(requestId, response.requestId());
        assertEquals(receiverId, response.receiverId());
        assertEquals(CaseStatus.OPEN, response.status());
        verify(verificationCaseRepository).save(any(VerificationCase.class));
    }

    @Test
    @DisplayName("openCase should return existing case if already opened")
    void shouldReturnExistingCaseWhenAlreadyExists() {
        when(verificationCaseRepository.existsByRequestIdAndDeletedAtIsNull(requestId)).thenReturn(true);
        when(verificationCaseRepository.findByRequestId(requestId)).thenReturn(Optional.of(sampleCase));

        VerificationCaseResponse response = verificationService.openCase(requestId, receiverId);

        assertEquals(caseId, response.caseId());
        verify(verificationCaseRepository, never()).save(any());
    }

    @Test
    @DisplayName("getCase should return active case by caseId")
    void shouldGetActiveCase() {
        when(verificationCaseRepository.findActiveById(caseId)).thenReturn(Optional.of(sampleCase));

        VerificationCaseResponse response = verificationService.getCase(caseId);

        assertNotNull(response);
        assertEquals(caseId, response.caseId());
    }

    @Test
    @DisplayName("getCase should throw VerificationCaseNotFoundException when not found")
    void shouldThrowWhenCaseNotFound() {
        when(verificationCaseRepository.findActiveById(caseId)).thenReturn(Optional.empty());

        assertThrows(VerificationCaseNotFoundException.class, () -> verificationService.getCase(caseId));
    }

    @Test
    @DisplayName("approveCase should update status to APPROVED and publish outbox event")
    void shouldApproveCaseAndEnqueueEvent() {
        when(verificationCaseRepository.findActiveById(caseId)).thenReturn(Optional.of(sampleCase));
        when(verificationCaseRepository.save(any(VerificationCase.class))).thenAnswer(inv -> inv.getArgument(0));

        ApproveVerificationRequest request = new ApproveVerificationRequest("All credentials verified");
        VerificationCaseResponse response = verificationService.approveCase(caseId, request, "coordinator-jane");

        assertNotNull(response);
        assertEquals(CaseStatus.APPROVED, response.status());
        assertEquals("coordinator-jane", response.reviewedBy());
        assertEquals("All credentials verified", response.reviewNotes());
        assertNotNull(response.resolvedAt());

        verify(outboxService).enqueue(
            eq(KafkaTopics.REQUEST_VERIFIED),
            eq("VERIFICATION_CASE"),
            eq(caseId),
            eq("RequestVerified"),
            any(),
            eq(requestId.toString())
        );
    }

    @Test
    @DisplayName("rejectCase should update status to REJECTED and publish outbox event")
    void shouldRejectCaseAndEnqueueEvent() {
        when(verificationCaseRepository.findActiveById(caseId)).thenReturn(Optional.of(sampleCase));
        when(verificationCaseRepository.save(any(VerificationCase.class))).thenAnswer(inv -> inv.getArgument(0));

        RejectVerificationRequest request = new RejectVerificationRequest("Documents expired");
        VerificationCaseResponse response = verificationService.rejectCase(caseId, request, "coordinator-bob");

        assertNotNull(response);
        assertEquals(CaseStatus.REJECTED, response.status());
        assertEquals("coordinator-bob", response.reviewedBy());
        assertEquals("Documents expired", response.reviewNotes());

        verify(outboxService).enqueue(
            eq(KafkaTopics.REQUEST_VERIFIED),
            eq("VERIFICATION_CASE"),
            eq(caseId),
            eq("RequestVerified"),
            any(),
            eq(requestId.toString())
        );
    }

    @Test
    @DisplayName("approveCase should throw InvalidCaseStatusException if already resolved")
    void shouldThrowWhenApprovingAlreadyResolvedCase() {
        sampleCase.setStatus(CaseStatus.APPROVED);
        when(verificationCaseRepository.findActiveById(caseId)).thenReturn(Optional.of(sampleCase));

        ApproveVerificationRequest request = new ApproveVerificationRequest("Re-approve");
        assertThrows(InvalidCaseStatusException.class, () -> verificationService.approveCase(caseId, request, "admin"));
    }

    @Test
    @DisplayName("uploadDocument should advance case status to UNDER_REVIEW and attach document")
    void shouldUploadDocumentAndTransitionStatus() {
        when(verificationCaseRepository.findActiveById(caseId)).thenReturn(Optional.of(sampleCase));
        when(verificationCaseRepository.save(any(VerificationCase.class))).thenAnswer(inv -> inv.getArgument(0));

        UploadDocumentRequest request = new UploadDocumentRequest(
            DocumentType.NATIONAL_ID,
            "https://cdn.relief.org/docs/id123.pdf"
        );

        VerificationCaseResponse response = verificationService.uploadDocument(caseId, request);

        assertNotNull(response);
        assertEquals(CaseStatus.UNDER_REVIEW, response.status());
        assertEquals(1, response.documents().size());
        assertEquals(DocumentType.NATIONAL_ID, response.documents().get(0).documentType());
    }

    @Test
    @DisplayName("listCases should return paginated list")
    void shouldListCases() {
        Pageable pageable = PageRequest.of(0, 10);
        when(verificationCaseRepository.findAllByStatus(CaseStatus.OPEN, pageable))
            .thenReturn(new PageImpl<>(List.of(sampleCase), pageable, 1));

        Page<VerificationCaseResponse> page = verificationService.listCases(CaseStatus.OPEN, pageable);

        assertEquals(1, page.getTotalElements());
        assertEquals(caseId, page.getContent().get(0).caseId());
    }
}
