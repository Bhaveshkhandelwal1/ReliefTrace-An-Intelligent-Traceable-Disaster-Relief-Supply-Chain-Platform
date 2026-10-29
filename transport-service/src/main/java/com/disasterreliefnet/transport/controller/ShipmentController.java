package com.disasterreliefnet.transport.controller;

import com.disasterreliefnet.transport.domain.ShipmentStatus;
import com.disasterreliefnet.transport.dto.*;
import com.disasterreliefnet.transport.service.ShipmentService;
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
@RequestMapping("/api/v1/transport/shipments")
@Tag(name = "Shipments", description = "Endpoints for relief goods dispatch, driver assignment, real-time tracking, and delivery confirmation")
public class ShipmentController {

    private final ShipmentService shipmentService;

    public ShipmentController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    @Operation(summary = "Create shipment manually", description = "Initiates a shipment for an approved aid allocation")
    @ApiResponse(responseCode = "201", description = "Shipment created successfully")
    @ApiResponse(responseCode = "409", description = "Shipment for this allocation already exists")
    public ResponseEntity<ShipmentResponse> createShipment(@Valid @RequestBody CreateShipmentRequest request) {
        ShipmentResponse response = shipmentService.createShipment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR', 'DRIVER')")
    @Operation(summary = "List shipments", description = "Retrieves paginated shipments filtered by status or driver")
    @ApiResponse(responseCode = "200", description = "Paginated shipments")
    public ResponseEntity<Page<ShipmentResponse>> listShipments(
        @RequestParam(required = false) ShipmentStatus status,
        @RequestParam(required = false) UUID driverId,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<ShipmentResponse> shipments = shipmentService.findShipments(status, driverId, pageable);
        return ResponseEntity.ok(shipments);
    }

    @GetMapping("/{shipmentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR', 'DRIVER', 'RECEIVER')")
    @Operation(summary = "Get shipment details", description = "Retrieves shipment metadata, delivery addresses, and current status")
    @ApiResponse(responseCode = "200", description = "Shipment found")
    @ApiResponse(responseCode = "404", description = "Shipment not found")
    public ResponseEntity<ShipmentResponse> getShipmentById(@PathVariable UUID shipmentId) {
        ShipmentResponse response = shipmentService.getShipmentById(shipmentId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{shipmentId}/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    @Operation(summary = "Assign driver and vehicle", description = "Assigns an available driver and active vehicle to the shipment")
    @ApiResponse(responseCode = "200", description = "Driver successfully assigned")
    @ApiResponse(responseCode = "404", description = "Shipment, driver, or vehicle not found")
    @ApiResponse(responseCode = "422", description = "Driver unavailable or vehicle mismatch")
    public ResponseEntity<ShipmentResponse> assignDriver(
        @PathVariable UUID shipmentId,
        @Valid @RequestBody AssignDriverRequest request
    ) {
        ShipmentResponse response = shipmentService.assignDriver(shipmentId, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{shipmentId}/pickup")
    @PreAuthorize("hasAnyRole('DRIVER', 'ADMIN')")
    @Operation(summary = "Mark supplies picked up", description = "Confirms that the driver has collected the relief goods from the warehouse")
    @ApiResponse(responseCode = "200", description = "Shipment status updated to PICKED_UP")
    @ApiResponse(responseCode = "403", description = "User is not assigned driver")
    @ApiResponse(responseCode = "422", description = "Shipment not in ASSIGNED state")
    public ResponseEntity<ShipmentResponse> pickupShipment(
        @PathVariable UUID shipmentId,
        Authentication authentication
    ) {
        String currentUserId = authentication != null ? authentication.getName() : null;
        ShipmentResponse response = shipmentService.pickupShipment(shipmentId, currentUserId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{shipmentId}/deliver")
    @PreAuthorize("hasAnyRole('DRIVER', 'ADMIN')")
    @Operation(summary = "Confirm shipment delivery", description = "Uploads proof of delivery and marks shipment as DELIVERED")
    @ApiResponse(responseCode = "200", description = "Shipment marked as DELIVERED")
    @ApiResponse(responseCode = "403", description = "User is not assigned driver")
    @ApiResponse(responseCode = "422", description = "Shipment not in PICKED_UP state")
    public ResponseEntity<ShipmentResponse> deliverShipment(
        @PathVariable UUID shipmentId,
        @Valid @RequestBody DeliverShipmentRequest request,
        Authentication authentication
    ) {
        String currentUserId = authentication != null ? authentication.getName() : null;
        ShipmentResponse response = shipmentService.deliverShipment(shipmentId, request, currentUserId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{shipmentId}/dispute")
    @PreAuthorize("hasAnyRole('DRIVER', 'RECEIVER', 'ADMIN')")
    @Operation(summary = "Dispute shipment delivery", description = "Flags an issue with delivery or recipient verification")
    @ApiResponse(responseCode = "200", description = "Shipment marked as DISPUTED")
    @ApiResponse(responseCode = "422", description = "Shipment cannot be disputed in current status")
    public ResponseEntity<ShipmentResponse> disputeShipment(
        @PathVariable UUID shipmentId,
        @Valid @RequestBody DisputeShipmentRequest request,
        Authentication authentication
    ) {
        String disputedBy = "COORDINATOR";
        if (authentication != null && authentication.getAuthorities() != null) {
            boolean isDriver = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_DRIVER"));
            boolean isReceiver = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_RECEIVER"));
            if (isDriver) {
                disputedBy = "DRIVER";
            } else if (isReceiver) {
                disputedBy = "RECEIVER";
            }
        }
        ShipmentResponse response = shipmentService.disputeShipment(shipmentId, request, disputedBy);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{shipmentId}/in-transit")
    @PreAuthorize("hasAnyRole('DRIVER', 'ADMIN')")
    @Operation(summary = "Mark supplies in transit", description = "Updates status to IN_TRANSIT when driver begins delivery route")
    @ApiResponse(responseCode = "200", description = "Shipment marked as IN_TRANSIT")
    @ApiResponse(responseCode = "403", description = "User is not assigned driver")
    @ApiResponse(responseCode = "422", description = "Shipment not in PICKED_UP state")
    public ResponseEntity<ShipmentResponse> startTransit(
        @PathVariable UUID shipmentId,
        Authentication authentication
    ) {
        String currentUserId = authentication != null ? authentication.getName() : null;
        ShipmentResponse response = shipmentService.startTransit(shipmentId, currentUserId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{shipmentId}/fail")
    @PreAuthorize("hasAnyRole('DRIVER', 'ADMIN', 'COORDINATOR')")
    @Operation(summary = "Fail shipment", description = "Flags shipment delivery as failed due to transit impediment, hazard, or vehicle fault")
    @ApiResponse(responseCode = "200", description = "Shipment marked as FAILED")
    @ApiResponse(responseCode = "422", description = "Shipment cannot be failed in terminal state")
    public ResponseEntity<ShipmentResponse> failShipment(
        @PathVariable UUID shipmentId,
        @Valid @RequestBody FailShipmentRequest request,
        Authentication authentication
    ) {
        String currentUserId = authentication != null ? authentication.getName() : null;
        ShipmentResponse response = shipmentService.failShipment(shipmentId, request.reason(), currentUserId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{shipmentId}/auto-assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    @Operation(summary = "Auto-assign shipment", description = "Automatically selects and assigns the best matching available driver and active vehicle")
    @ApiResponse(responseCode = "200", description = "Driver automatically assigned")
    @ApiResponse(responseCode = "422", description = "No available driver or vehicle found")
    public ResponseEntity<ShipmentResponse> autoAssign(
        @PathVariable UUID shipmentId,
        @RequestParam(required = false) UUID disasterId
    ) {
        ShipmentResponse response = shipmentService.autoAssignShipment(shipmentId, disasterId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{shipmentId}/tracking")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR', 'DRIVER', 'RECEIVER')")
    @Operation(summary = "Get tracking timeline", description = "Retrieves real-time tracking timeline and events for the shipment")
    @ApiResponse(responseCode = "200", description = "Tracking timeline retrieved")
    @ApiResponse(responseCode = "404", description = "Shipment not found")
    public ResponseEntity<TrackingResponse> getTracking(@PathVariable UUID shipmentId) {
        TrackingResponse response = shipmentService.getTracking(shipmentId);
        return ResponseEntity.ok(response);
    }
}
