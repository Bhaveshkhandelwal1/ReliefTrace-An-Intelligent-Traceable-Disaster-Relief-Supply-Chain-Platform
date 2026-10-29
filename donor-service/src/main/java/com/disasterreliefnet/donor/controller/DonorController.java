package com.disasterreliefnet.donor.controller;

import com.disasterreliefnet.donor.client.dto.VerificationStatusDto;
import com.disasterreliefnet.donor.dto.*;
import com.disasterreliefnet.donor.service.DonorService;
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
@RequestMapping("/api/v1/donors")
@Tag(name = "Donors", description = "Endpoints for donor registration, profile management, and impact analytics")
public class DonorController {

    private final DonorService donorService;

    public DonorController(DonorService donorService) {
        this.donorService = donorService;
    }

    @PostMapping
    @Operation(summary = "Register donor profile", description = "Public or authenticated registration for individual, corporate, NGO, or pharmacy donors")
    @ApiResponse(responseCode = "201", description = "Donor registered successfully")
    @ApiResponse(responseCode = "409", description = "Email already registered")
    public ResponseEntity<DonorResponse> createDonor(@Valid @RequestBody CreateDonorRequest request) {
        DonorResponse response = donorService.createDonor(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{donorId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR') or #donorId.toString() == authentication.name")
    @Operation(summary = "Get donor profile", description = "Retrieves donor details by ID")
    @ApiResponse(responseCode = "200", description = "Donor found")
    @ApiResponse(responseCode = "404", description = "Donor not found")
    public ResponseEntity<DonorResponse> getDonorById(@PathVariable UUID donorId) {
        DonorResponse response = donorService.getDonorById(donorId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{donorId}")
    @PreAuthorize("hasRole('ADMIN') or #donorId.toString() == authentication.name")
    @Operation(summary = "Update donor profile", description = "Updates details, address, or tax ID for a donor")
    @ApiResponse(responseCode = "200", description = "Donor updated successfully")
    @ApiResponse(responseCode = "404", description = "Donor not found")
    public ResponseEntity<DonorResponse> updateDonor(
        @PathVariable UUID donorId,
        @Valid @RequestBody UpdateDonorRequest request
    ) {
        DonorResponse response = donorService.updateDonor(donorId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{donorId}")
    @PreAuthorize("hasRole('ADMIN') or #donorId.toString() == authentication.name")
    @Operation(summary = "Deactivate donor profile", description = "Soft deletes a donor account")
    @ApiResponse(responseCode = "204", description = "Donor account deleted")
    @ApiResponse(responseCode = "404", description = "Donor not found")
    public ResponseEntity<Void> deleteDonor(@PathVariable UUID donorId) {
        donorService.deleteDonor(donorId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    @Operation(summary = "List donors", description = "Retrieves paged donors directory")
    @ApiResponse(responseCode = "200", description = "Donors retrieved successfully")
    public ResponseEntity<Page<DonorResponse>> getDonors(@PageableDefault(size = 20) Pageable pageable) {
        Page<DonorResponse> page = donorService.getDonors(pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{donorId}/impact")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR') or #donorId.toString() == authentication.name")
    @Operation(summary = "Get donor impact summary", description = "Calculates total contributions and delivered shipments impact")
    @ApiResponse(responseCode = "200", description = "Impact summary calculated")
    public ResponseEntity<DonorImpactSummaryDto> getDonorImpact(@PathVariable UUID donorId) {
        DonorImpactSummaryDto impact = donorService.getDonorImpactSummary(donorId);
        return ResponseEntity.ok(impact);
    }

    @GetMapping("/{donorId}/verification")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    @Operation(summary = "Check donor verification status", description = "Calls Verification Service synchronously via Feign client")
    @ApiResponse(responseCode = "200", description = "Verification status retrieved")
    public ResponseEntity<VerificationStatusDto> checkVerification(@PathVariable UUID donorId) {
        VerificationStatusDto status = donorService.checkDonorVerification(donorId);
        return ResponseEntity.ok(status);
    }
}
