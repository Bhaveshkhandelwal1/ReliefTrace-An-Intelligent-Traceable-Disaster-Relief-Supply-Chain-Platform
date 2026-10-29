package com.disasterreliefnet.matching.controller;

import com.disasterreliefnet.common.enums.AllocationStatus;
import com.disasterreliefnet.matching.dto.*;
import com.disasterreliefnet.matching.service.MatchingService;
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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/matching")
@Tag(name = "Matching", description = "Endpoints for AI/algorithmic supply matching, candidate suggestions, and coordinator allocation approval")
public class MatchingController {

    private final MatchingService matchingService;

    public MatchingController(MatchingService matchingService) {
        this.matchingService = matchingService;
    }

    @GetMapping("/suggestions")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    @Operation(summary = "Generate allocation suggestions", description = "Scores available warehouse items against verified demand and returns ranked recommendations")
    @ApiResponse(responseCode = "200", description = "Ranked list of allocation suggestions")
    public ResponseEntity<List<AllocationSuggestionResponse>> getSuggestions(
        @RequestParam UUID requestId
    ) {
        List<AllocationSuggestionResponse> suggestions = matchingService.generateSuggestions(requestId);
        return ResponseEntity.ok(suggestions);
    }

    @PostMapping("/allocations")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    @Operation(summary = "Create allocation proposal", description = "Proposes a new supply allocation for a verified aid request")
    @ApiResponse(responseCode = "201", description = "Allocation proposed successfully")
    @ApiResponse(responseCode = "422", description = "Insufficient inventory stock")
    public ResponseEntity<AllocationResponse> createAllocation(
        @Valid @RequestBody CreateAllocationRequest request
    ) {
        AllocationResponse response = matchingService.createAllocation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/allocations")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    @Operation(summary = "List allocations", description = "Retrieves paginated list of allocations filtered by status or disasterId")
    @ApiResponse(responseCode = "200", description = "Paginated list of allocations")
    public ResponseEntity<Page<AllocationResponse>> listAllocations(
        @RequestParam(required = false) AllocationStatus status,
        @RequestParam(required = false) UUID disasterId,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<AllocationResponse> page = matchingService.listAllocations(status, disasterId, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/allocations/{allocationId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    @Operation(summary = "Get allocation details", description = "Retrieves a single allocation by ID")
    @ApiResponse(responseCode = "200", description = "Allocation found")
    @ApiResponse(responseCode = "404", description = "Allocation not found")
    public ResponseEntity<AllocationResponse> getAllocation(@PathVariable UUID allocationId) {
        AllocationResponse response = matchingService.getAllocation(allocationId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/allocations/{allocationId}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    @Operation(summary = "Approve allocation", description = "Coordinator confirms allocation, initiating stock reservation saga")
    @ApiResponse(responseCode = "200", description = "Allocation approved successfully")
    @ApiResponse(responseCode = "404", description = "Allocation not found")
    @ApiResponse(responseCode = "422", description = "Cannot approve in current status")
    public ResponseEntity<AllocationResponse> approveAllocation(
        @PathVariable UUID allocationId,
        @Valid @RequestBody ApproveAllocationRequest request,
        Authentication authentication
    ) {
        String reviewer = authentication != null ? authentication.getName() : "coordinator";
        AllocationResponse response = matchingService.approveAllocation(allocationId, request, reviewer);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/allocations/{allocationId}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    @Operation(summary = "Reject allocation", description = "Coordinator rejects proposed allocation with mandatory reason")
    @ApiResponse(responseCode = "200", description = "Allocation rejected")
    @ApiResponse(responseCode = "404", description = "Allocation not found")
    @ApiResponse(responseCode = "422", description = "Cannot reject in current status")
    public ResponseEntity<AllocationResponse> rejectAllocation(
        @PathVariable UUID allocationId,
        @Valid @RequestBody RejectAllocationRequest request,
        Authentication authentication
    ) {
        String reviewer = authentication != null ? authentication.getName() : "coordinator";
        AllocationResponse response = matchingService.rejectAllocation(allocationId, request, reviewer);
        return ResponseEntity.ok(response);
    }
}
