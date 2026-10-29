package com.disasterreliefnet.disaster.controller;

import com.disasterreliefnet.common.enums.DisasterStatus;
import com.disasterreliefnet.common.enums.DisasterType;
import com.disasterreliefnet.disaster.dto.*;
import com.disasterreliefnet.disaster.service.DisasterService;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/disasters")
@Tag(name = "Disasters", description = "Endpoints for managing disaster incidents, alerts, and situational updates")
public class DisasterController {

    private final DisasterService disasterService;

    public DisasterController(DisasterService disasterService) {
        this.disasterService = disasterService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    @Operation(summary = "Declare a disaster", description = "Declares a new disaster incident with geospatial coverage")
    @ApiResponse(responseCode = "201", description = "Disaster declared successfully")
    public ResponseEntity<DisasterResponse> createDisaster(
        @Valid @RequestBody CreateDisasterRequest request,
        Authentication authentication
    ) {
        String createdBy = authentication != null ? authentication.getName() : "system";
        DisasterResponse response = disasterService.createDisaster(request, createdBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "List disasters", description = "Retrieves paged disasters with optional status and type filters")
    @ApiResponse(responseCode = "200", description = "Disasters retrieved successfully")
    public ResponseEntity<Page<DisasterResponse>> getDisasters(
        @RequestParam(required = false) DisasterStatus status,
        @RequestParam(required = false) DisasterType type,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<DisasterResponse> page = disasterService.getDisasters(status, type, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{disasterId}")
    @Operation(summary = "Get disaster by ID", description = "Retrieves detailed information about a disaster")
    @ApiResponse(responseCode = "200", description = "Disaster found")
    @ApiResponse(responseCode = "404", description = "Disaster not found")
    public ResponseEntity<DisasterResponse> getDisasterById(@PathVariable UUID disasterId) {
        DisasterResponse response = disasterService.getDisasterById(disasterId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{disasterId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    @Operation(summary = "Update disaster", description = "Updates severity, description, or casualties of an active disaster")
    @ApiResponse(responseCode = "200", description = "Disaster updated successfully")
    @ApiResponse(responseCode = "404", description = "Disaster not found")
    public ResponseEntity<DisasterResponse> updateDisaster(
        @PathVariable UUID disasterId,
        @Valid @RequestBody UpdateDisasterRequest request,
        Authentication authentication
    ) {
        String updatedBy = authentication != null ? authentication.getName() : "system";
        DisasterResponse response = disasterService.updateDisaster(disasterId, request, updatedBy);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{disasterId}/close")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Close disaster", description = "Closes an active disaster incident with closure reason")
    @ApiResponse(responseCode = "200", description = "Disaster closed successfully")
    @ApiResponse(responseCode = "404", description = "Disaster not found")
    public ResponseEntity<DisasterResponse> closeDisaster(
        @PathVariable UUID disasterId,
        @Valid @RequestBody CloseDisasterRequest request,
        Authentication authentication
    ) {
        String closedBy = authentication != null ? authentication.getName() : "admin";
        DisasterResponse response = disasterService.closeDisaster(disasterId, request, closedBy);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{disasterId}/updates")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    @Operation(summary = "Add disaster update", description = "Posts a situational update or advisory for a disaster")
    @ApiResponse(responseCode = "201", description = "Disaster update created successfully")
    @ApiResponse(responseCode = "404", description = "Disaster not found")
    public ResponseEntity<DisasterUpdateResponse> addDisasterUpdate(
        @PathVariable UUID disasterId,
        @Valid @RequestBody CreateDisasterUpdateRequest request,
        Authentication authentication
    ) {
        String postedBy = authentication != null ? authentication.getName() : "system";
        DisasterUpdateResponse response = disasterService.addDisasterUpdate(disasterId, request, postedBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{disasterId}/updates")
    @Operation(summary = "Get disaster updates", description = "Retrieves situational updates for a disaster")
    @ApiResponse(responseCode = "200", description = "Updates retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Disaster not found")
    public ResponseEntity<Page<DisasterUpdateResponse>> getDisasterUpdates(
        @PathVariable UUID disasterId,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<DisasterUpdateResponse> page = disasterService.getDisasterUpdates(disasterId, pageable);
        return ResponseEntity.ok(page);
    }
}
