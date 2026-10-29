package com.disasterreliefnet.finance.controller;

import com.disasterreliefnet.common.enums.PaymentStatus;
import com.disasterreliefnet.finance.dto.*;
import com.disasterreliefnet.finance.service.DisbursementService;
import com.disasterreliefnet.finance.service.FinanceSummaryService;
import com.disasterreliefnet.finance.service.LedgerService;
import com.disasterreliefnet.finance.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/finance")
@Tag(name = "Finance", description = "Endpoints for payment processing, disbursements, general ledger, and financial summaries")
public class FinanceController {

    private final PaymentService paymentService;
    private final DisbursementService disbursementService;
    private final LedgerService ledgerService;
    private final FinanceSummaryService financeSummaryService;

    public FinanceController(
        PaymentService paymentService,
        DisbursementService disbursementService,
        LedgerService ledgerService,
        FinanceSummaryService financeSummaryService
    ) {
        this.paymentService = paymentService;
        this.disbursementService = disbursementService;
        this.ledgerService = ledgerService;
        this.financeSummaryService = financeSummaryService;
    }

    // -----------------------------------------------------------------------
    // Payments
    // -----------------------------------------------------------------------

    @GetMapping("/payments")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    @Operation(summary = "List payment records", description = "Retrieves paginated payments filtered by donation or status")
    @ApiResponse(responseCode = "200", description = "Paginated list of payments")
    public ResponseEntity<Page<PaymentRecordResponse>> listPayments(
        @RequestParam(required = false) UUID donationId,
        @RequestParam(required = false) PaymentStatus status,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<PaymentRecordResponse> payments = paymentService.findPayments(donationId, status, pageable);
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/payments/{paymentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR', 'DONOR')")
    @Operation(summary = "Get payment details", description = "Retrieves payment details by unique identifier")
    @ApiResponse(responseCode = "200", description = "Payment record found")
    @ApiResponse(responseCode = "404", description = "Payment record not found")
    public ResponseEntity<PaymentRecordResponse> getPaymentById(@PathVariable UUID paymentId) {
        PaymentRecordResponse payment = paymentService.getPaymentById(paymentId);
        return ResponseEntity.ok(payment);
    }

    @PostMapping("/payments/{paymentId}/process")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Process payment charge", description = "Charges external payment gateway and records credit in platform ledger")
    @ApiResponse(responseCode = "200", description = "Payment processed successfully")
    @ApiResponse(responseCode = "404", description = "Payment record not found")
    @ApiResponse(responseCode = "422", description = "Payment charge failed or already completed")
    public ResponseEntity<PaymentRecordResponse> processPayment(
        @PathVariable UUID paymentId,
        @Valid @RequestBody ProcessPaymentRequest request
    ) {
        PaymentRecordResponse response = paymentService.processPayment(paymentId, request);
        return ResponseEntity.ok(response);
    }

    // -----------------------------------------------------------------------
    // Disbursements
    // -----------------------------------------------------------------------

    @GetMapping("/disbursements")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    @Operation(summary = "List disbursements", description = "Retrieves paginated aid disbursements")
    @ApiResponse(responseCode = "200", description = "Paginated list of disbursements")
    public ResponseEntity<Page<DisbursementResponse>> listDisbursements(
        @RequestParam(required = false) UUID allocationId,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<DisbursementResponse> disbursements = disbursementService.findDisbursements(allocationId, pageable);
        return ResponseEntity.ok(disbursements);
    }

    @GetMapping("/disbursements/{disbursementId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    @Operation(summary = "Get disbursement details", description = "Retrieves disbursement payout details by ID")
    @ApiResponse(responseCode = "200", description = "Disbursement found")
    @ApiResponse(responseCode = "404", description = "Disbursement not found")
    public ResponseEntity<DisbursementResponse> getDisbursementById(@PathVariable UUID disbursementId) {
        DisbursementResponse response = disbursementService.getDisbursementById(disbursementId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/disbursements/{disbursementId}/authorize")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Authorize disbursement payout", description = "Approves release of relief funds and writes debit to ledger")
    @ApiResponse(responseCode = "200", description = "Disbursement authorized and marked as PAID")
    @ApiResponse(responseCode = "404", description = "Disbursement not found")
    @ApiResponse(responseCode = "422", description = "Disbursement in invalid status for authorization")
    public ResponseEntity<DisbursementResponse> authorizeDisbursement(
        @PathVariable UUID disbursementId,
        @Valid @RequestBody AuthorizeDisbursementRequest request
    ) {
        DisbursementResponse response = disbursementService.authorizeDisbursement(disbursementId, request);
        return ResponseEntity.ok(response);
    }

    // -----------------------------------------------------------------------
    // Ledger & Summary
    // -----------------------------------------------------------------------

    @GetMapping("/ledger")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Query double-entry ledger", description = "Retrieves audit-trail ledger transactions within time window")
    @ApiResponse(responseCode = "200", description = "Paginated list of ledger entries")
    public ResponseEntity<Page<LedgerEntryResponse>> getLedger(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<LedgerEntryResponse> entries = ledgerService.getLedgerEntries(fromDate, toDate, pageable);
        return ResponseEntity.ok(entries);
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    @Operation(summary = "Financial summary dashboard", description = "Aggregates total donations, total disbursements, and net liquid balance")
    @ApiResponse(responseCode = "200", description = "Financial summary statistics")
    public ResponseEntity<FinanceSummaryResponse> getSummary(
        @RequestParam(required = false) UUID disasterId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate
    ) {
        FinanceSummaryResponse summary = financeSummaryService.getSummary(fromDate, toDate);
        return ResponseEntity.ok(summary);
    }
}
