package com.disasterreliefnet.transport.controller;

import com.disasterreliefnet.transport.dto.CreateVehicleRequest;
import com.disasterreliefnet.transport.dto.VehicleResponse;
import com.disasterreliefnet.transport.service.VehicleService;
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
@RequestMapping("/api/v1/transport/vehicles")
@Tag(name = "Vehicles", description = "Endpoints for transport fleet vehicle registration, capacity specs, and assignment")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Register vehicle", description = "Adds a vehicle specification and associates it with a driver")
    @ApiResponse(responseCode = "201", description = "Vehicle registered successfully")
    @ApiResponse(responseCode = "404", description = "Driver not found")
    @ApiResponse(responseCode = "409", description = "Vehicle plate number already registered")
    public ResponseEntity<VehicleResponse> registerVehicle(@Valid @RequestBody CreateVehicleRequest request) {
        VehicleResponse response = vehicleService.registerVehicle(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    @Operation(summary = "List vehicles", description = "Retrieves paginated vehicles, optionally filtered by assigned driver")
    @ApiResponse(responseCode = "200", description = "Paginated list of vehicles")
    public ResponseEntity<Page<VehicleResponse>> listVehicles(
        @RequestParam(required = false) UUID driverId,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<VehicleResponse> vehicles = vehicleService.findVehicles(driverId, pageable);
        return ResponseEntity.ok(vehicles);
    }

    @GetMapping("/{vehicleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR', 'DRIVER')")
    @Operation(summary = "Get vehicle by ID", description = "Retrieves vehicle details and carrying capacities")
    @ApiResponse(responseCode = "200", description = "Vehicle found")
    @ApiResponse(responseCode = "404", description = "Vehicle not found")
    public ResponseEntity<VehicleResponse> getVehicleById(@PathVariable UUID vehicleId) {
        VehicleResponse response = vehicleService.getVehicleById(vehicleId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{vehicleId}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate vehicle", description = "Marks a vehicle as unavailable for future dispatches")
    @ApiResponse(responseCode = "200", description = "Vehicle deactivated")
    @ApiResponse(responseCode = "404", description = "Vehicle not found")
    public ResponseEntity<VehicleResponse> deactivateVehicle(@PathVariable UUID vehicleId) {
        VehicleResponse response = vehicleService.deactivateVehicle(vehicleId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{vehicleId}/breakdown")
    @PreAuthorize("hasAnyRole('DRIVER', 'ADMIN', 'COORDINATOR')")
    @Operation(summary = "Report vehicle breakdown", description = "Deactivates vehicle and triggers automatic reassignment of active shipments")
    @ApiResponse(responseCode = "200", description = "Breakdown recorded and shipments reassigned")
    @ApiResponse(responseCode = "404", description = "Vehicle not found")
    public ResponseEntity<VehicleResponse> reportBreakdown(
        @PathVariable UUID vehicleId,
        @Valid @RequestBody com.disasterreliefnet.transport.dto.BreakdownRequest request
    ) {
        VehicleResponse response = vehicleService.reportBreakdown(vehicleId, request);
        return ResponseEntity.ok(response);
    }
}
