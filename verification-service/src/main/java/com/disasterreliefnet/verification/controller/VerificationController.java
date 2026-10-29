package com.disasterreliefnet.verification.controller;

import com.disasterreliefnet.common.enums.CaseStatus;
import com.disasterreliefnet.verification.dto.*;
import com.disasterreliefnet.verification.service.VerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/verification/cases")
@Tag(name = "Verification", description = "Endpoints for coordinator review, document uploads, and case verification")
public class VerificationController {

    private final VerificationService verificationService;

    public VerificationController(VerificationService verificationService) {
        this.verificationService = verificationService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    @Operation(summary = "List verification cases", description = "Lists active verification cases with optional status filter and pagination")
    @ApiResponse(responseCode = "200", description = "Paginated list of verification cases")
    public ResponseEntity<Page<VerificationCaseResponse>> listCases(
        @RequestParam(required = false) CaseStatus status,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<VerificationCaseResponse> page = verificationService.listCases(status, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{caseId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    @Operation(summary = "Get verification case", description = "Retrieves a single verification case by caseId")
    @ApiResponse(responseCode = "200", description = "Verification case found")
    @ApiResponse(responseCode = "404", description = "Case not found")
    public ResponseEntity<VerificationCaseResponse> getCase(@PathVariable UUID caseId) {
        VerificationCaseResponse response = verificationService.getCase(caseId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/by-request/{requestId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    @Operation(summary = "Get case by requestId", description = "Retrieves verification case details associated with a specific aid request")
    @ApiResponse(responseCode = "200", description = "Verification case found")
    @ApiResponse(responseCode = "404", description = "Case not found for request")
    public ResponseEntity<VerificationCaseResponse> getCaseByRequestId(@PathVariable UUID requestId) {
        VerificationCaseResponse response = verificationService.getCaseByRequestId(requestId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{caseId}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    @Operation(summary = "Approve verification case", description = "Approves a pending case and triggers request status transition")
    @ApiResponse(responseCode = "200", description = "Verification case approved")
    @ApiResponse(responseCode = "404", description = "Case not found")
    @ApiResponse(responseCode = "422", description = "Cannot approve already resolved case")
    public ResponseEntity<VerificationCaseResponse> approveCase(
        @PathVariable UUID caseId,
        @Valid @RequestBody ApproveVerificationRequest request,
        Authentication authentication
    ) {
        String reviewer = authentication != null ? authentication.getName() : "coordinator";
        VerificationCaseResponse response = verificationService.approveCase(caseId, request, reviewer);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{caseId}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    @Operation(summary = "Reject verification case", description = "Rejects a verification case with mandatory review notes")
    @ApiResponse(responseCode = "200", description = "Verification case rejected")
    @ApiResponse(responseCode = "404", description = "Case not found")
    @ApiResponse(responseCode = "422", description = "Cannot reject already resolved case")
    public ResponseEntity<VerificationCaseResponse> rejectCase(
        @PathVariable UUID caseId,
        @Valid @RequestBody RejectVerificationRequest request,
        Authentication authentication
    ) {
        String reviewer = authentication != null ? authentication.getName() : "coordinator";
        VerificationCaseResponse response = verificationService.rejectCase(caseId, request, reviewer);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{caseId}/documents")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR', 'RECEIVER')")
    @Operation(summary = "Upload verification document", description = "Attaches supporting document (national ID, proof of address) to case")
    @ApiResponse(responseCode = "200", description = "Document attached to case")
    @ApiResponse(responseCode = "404", description = "Case not found")
    @ApiResponse(responseCode = "422", description = "Cannot upload documents to resolved case")
    public ResponseEntity<VerificationCaseResponse> uploadDocument(
        @PathVariable UUID caseId,
        @Valid @RequestBody UploadDocumentRequest request
    ) {
        VerificationCaseResponse response = verificationService.uploadDocument(caseId, request);
        return ResponseEntity.ok(response);
    }
}
