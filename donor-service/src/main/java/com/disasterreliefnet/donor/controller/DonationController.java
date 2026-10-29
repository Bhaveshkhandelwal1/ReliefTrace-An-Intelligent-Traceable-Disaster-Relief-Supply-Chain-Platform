package com.disasterreliefnet.donor.controller;

import com.disasterreliefnet.common.enums.DonationStatus;
import com.disasterreliefnet.donor.dto.*;
import com.disasterreliefnet.donor.service.DonationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/donors/{donorId}/donations")
@Tag(name = "Donations", description = "Endpoints for monetary donations, in-kind pledges, item lines, and tax receipts")
public class DonationController {

    private final DonationService donationService;

    public DonationController(DonationService donationService) {
        this.donationService = donationService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or #donorId.toString() == authentication.name")
    @Operation(summary = "Pledge or register donation", description = "Submits a new financial donation or in-kind pledge with optional item lines")
    @ApiResponse(responseCode = "201", description = "Donation registered successfully")
    @ApiResponse(responseCode = "404", description = "Donor not found")
    public ResponseEntity<DonationResponse> createDonation(
        @PathVariable UUID donorId,
        @Valid @RequestBody CreateDonationRequest request
    ) {
        DonationResponse response = donationService.createDonation(donorId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR') or #donorId.toString() == authentication.name")
    @Operation(summary = "Get donor donations history", description = "Paged listing of donations made by a donor")
    @ApiResponse(responseCode = "200", description = "Donations history retrieved")
    public ResponseEntity<Page<DonationResponse>> getDonations(
        @PathVariable UUID donorId,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<DonationResponse> page = donationService.getDonationsByDonor(donorId, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{donationId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR') or #donorId.toString() == authentication.name")
    @Operation(summary = "Get donation details", description = "Retrieves specific donation by ID")
    @ApiResponse(responseCode = "200", description = "Donation details found")
    @ApiResponse(responseCode = "404", description = "Donation not found")
    public ResponseEntity<DonationResponse> getDonationById(
        @PathVariable UUID donorId,
        @PathVariable UUID donationId
    ) {
        DonationResponse response = donationService.getDonationById(donorId, donationId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{donationId}/cancel")
    @PreAuthorize("hasRole('ADMIN') or #donorId.toString() == authentication.name")
    @Operation(summary = "Cancel donation pledge", description = "Cancels a pending or pledged donation")
    @ApiResponse(responseCode = "200", description = "Donation cancelled successfully")
    @ApiResponse(responseCode = "400", description = "Donation cannot be cancelled")
    @ApiResponse(responseCode = "404", description = "Donation not found")
    public ResponseEntity<DonationResponse> cancelDonation(
        @PathVariable UUID donorId,
        @PathVariable UUID donationId
    ) {
        DonationResponse response = donationService.cancelDonation(donorId, donationId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{donationId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    @Operation(summary = "Transition donation status", description = "Updates pledge status lifecycle (PLEDGED, RECEIVED, REJECTED)")
    @ApiResponse(responseCode = "200", description = "Donation status updated")
    @ApiResponse(responseCode = "404", description = "Donation not found")
    public ResponseEntity<DonationResponse> transitionStatus(
        @PathVariable UUID donorId,
        @PathVariable UUID donationId,
        @RequestParam DonationStatus status,
        @RequestParam(required = false) String notes
    ) {
        DonationResponse response = donationService.transitionDonationStatus(donationId, status, notes);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{donationId}/receipt")
    @PreAuthorize("hasRole('ADMIN') or #donorId.toString() == authentication.name")
    @Operation(summary = "Download donation receipt", description = "Retrieves official tax receipt for completed/received donation")
    @ApiResponse(responseCode = "200", description = "Receipt retrieved")
    @ApiResponse(responseCode = "404", description = "Donation not found")
    public ResponseEntity<DonationReceiptDto> getReceipt(
        @PathVariable UUID donorId,
        @PathVariable UUID donationId
    ) {
        DonationReceiptDto receipt = donationService.getDonationReceipt(donorId, donationId);
        return ResponseEntity.ok(receipt);
    }
}
