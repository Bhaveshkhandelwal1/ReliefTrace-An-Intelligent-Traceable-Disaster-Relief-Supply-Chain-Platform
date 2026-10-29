package com.disasterreliefnet.receiver.controller;

import com.disasterreliefnet.common.enums.RequestStatus;
import com.disasterreliefnet.receiver.dto.*;
import com.disasterreliefnet.receiver.service.ReceiverService;
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
@RequestMapping("/api/v1/receivers")
@Tag(name = "Receivers", description = "Endpoints for disaster victim registration, profile management, and aid request submission")
public class ReceiverController {

    private final ReceiverService receiverService;

    public ReceiverController(ReceiverService receiverService) {
        this.receiverService = receiverService;
    }

    @PostMapping
    @Operation(summary = "Register receiver profile", description = "Public or authenticated self-registration for disaster victims/beneficiaries")
    @ApiResponse(responseCode = "201", description = "Receiver profile registered successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request fields")
    public ResponseEntity<ReceiverResponse> registerReceiver(@Valid @RequestBody CreateReceiverRequest request) {
        ReceiverResponse response = receiverService.registerReceiver(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{receiverId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR') or #receiverId.toString() == authentication.name")
    @Operation(summary = "Get receiver profile", description = "Retrieves beneficiary details by ID")
    @ApiResponse(responseCode = "200", description = "Receiver profile found")
    @ApiResponse(responseCode = "404", description = "Receiver not found")
    public ResponseEntity<ReceiverResponse> getReceiver(@PathVariable UUID receiverId) {
        ReceiverResponse response = receiverService.getReceiver(receiverId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{receiverId}")
    @PreAuthorize("hasRole('ADMIN') or #receiverId.toString() == authentication.name")
    @Operation(summary = "Update receiver profile", description = "Updates address, contact, household size or vulnerability tags")
    @ApiResponse(responseCode = "200", description = "Receiver profile updated successfully")
    @ApiResponse(responseCode = "404", description = "Receiver not found")
    public ResponseEntity<ReceiverResponse> updateReceiver(
        @PathVariable UUID receiverId,
        @Valid @RequestBody UpdateReceiverRequest request
    ) {
        ReceiverResponse response = receiverService.updateReceiver(receiverId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{receiverId}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Soft delete receiver", description = "Soft-deletes a receiver profile")
    @ApiResponse(responseCode = "204", description = "Receiver soft deleted")
    @ApiResponse(responseCode = "404", description = "Receiver not found")
    public ResponseEntity<Void> deleteReceiver(@PathVariable UUID receiverId) {
        receiverService.deleteReceiver(receiverId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    @Operation(summary = "List receivers", description = "Lists active receivers with optional disasterId filter and pagination")
    @ApiResponse(responseCode = "200", description = "Paginated list of receivers")
    public ResponseEntity<Page<ReceiverResponse>> listReceivers(
        @RequestParam(required = false) UUID disasterId,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<ReceiverResponse> page = receiverService.listReceivers(disasterId, pageable);
        return ResponseEntity.ok(page);
    }

    // -------------------------------------------------------------------------
    // Nested Aid Request endpoints
    // -------------------------------------------------------------------------

    @PostMapping("/{receiverId}/requests")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR') or #receiverId.toString() == authentication.name")
    @Operation(summary = "Create aid request", description = "Submits a new aid request for a specific receiver")
    @ApiResponse(responseCode = "201", description = "Aid request created successfully")
    @ApiResponse(responseCode = "409", description = "Duplicate pending request detected for same category and unit")
    public ResponseEntity<AidRequestResponse> createAidRequest(
        @PathVariable UUID receiverId,
        @Valid @RequestBody CreateAidRequestRequest request
    ) {
        AidRequestResponse response = receiverService.createAidRequest(receiverId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{receiverId}/requests")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR') or #receiverId.toString() == authentication.name")
    @Operation(summary = "List receiver aid requests", description = "Lists aid requests submitted by a receiver with optional status filter")
    @ApiResponse(responseCode = "200", description = "Paginated list of aid requests")
    public ResponseEntity<Page<AidRequestResponse>> listAidRequests(
        @PathVariable UUID receiverId,
        @RequestParam(required = false) RequestStatus status,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<AidRequestResponse> page = receiverService.listAidRequests(receiverId, status, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{receiverId}/requests/{requestId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR') or #receiverId.toString() == authentication.name")
    @Operation(summary = "Get single aid request", description = "Retrieves an aid request by ID scoped to the receiver")
    @ApiResponse(responseCode = "200", description = "Aid request found")
    @ApiResponse(responseCode = "404", description = "Aid request not found")
    public ResponseEntity<AidRequestResponse> getAidRequest(
        @PathVariable UUID receiverId,
        @PathVariable UUID requestId
    ) {
        AidRequestResponse response = receiverService.getAidRequest(receiverId, requestId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{receiverId}/requests/{requestId}/cancel")
    @PreAuthorize("hasRole('ADMIN') or #receiverId.toString() == authentication.name")
    @Operation(summary = "Cancel aid request", description = "Cancels a pending or unallocated aid request")
    @ApiResponse(responseCode = "200", description = "Aid request cancelled successfully")
    @ApiResponse(responseCode = "422", description = "Cannot cancel request in current status")
    public ResponseEntity<AidRequestResponse> cancelAidRequest(
        @PathVariable UUID receiverId,
        @PathVariable UUID requestId
    ) {
        AidRequestResponse response = receiverService.cancelAidRequest(receiverId, requestId);
        return ResponseEntity.ok(response);
    }
}
