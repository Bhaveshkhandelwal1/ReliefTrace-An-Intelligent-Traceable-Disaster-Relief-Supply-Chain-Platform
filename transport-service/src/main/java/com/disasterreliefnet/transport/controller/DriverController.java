package com.disasterreliefnet.transport.controller;

import com.disasterreliefnet.transport.dto.CreateDriverRequest;
import com.disasterreliefnet.transport.dto.DriverResponse;
import com.disasterreliefnet.transport.dto.UpdateDriverRequest;
import com.disasterreliefnet.transport.service.DriverService;
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
@RequestMapping("/api/v1/transport/drivers")
@Tag(name = "Drivers", description = "Endpoints for transport driver registration and fleet roster management")
public class DriverController {

    private final DriverService driverService;

    public DriverController(DriverService driverService) {
        this.driverService = driverService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Register a new driver", description = "Onboards a driver profile into the transport network")
    @ApiResponse(responseCode = "201", description = "Driver successfully registered")
    @ApiResponse(responseCode = "409", description = "Driver email or user ID already registered")
    public ResponseEntity<DriverResponse> registerDriver(@Valid @RequestBody CreateDriverRequest request) {
        DriverResponse response = driverService.registerDriver(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    @Operation(summary = "List drivers", description = "Retrieves paginated drivers, optionally filtered by availability")
    @ApiResponse(responseCode = "200", description = "Paginated list of drivers")
    public ResponseEntity<Page<DriverResponse>> listDrivers(
        @RequestParam(required = false) Boolean available,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<DriverResponse> drivers = driverService.findDrivers(available, pageable);
        return ResponseEntity.ok(drivers);
    }

    @GetMapping("/{driverId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR', 'DRIVER')")
    @Operation(summary = "Get driver by ID", description = "Retrieves driver details by unique driver identifier")
    @ApiResponse(responseCode = "200", description = "Driver details found")
    @ApiResponse(responseCode = "404", description = "Driver not found")
    public ResponseEntity<DriverResponse> getDriverById(@PathVariable UUID driverId) {
        DriverResponse driver = driverService.getDriverById(driverId);
        return ResponseEntity.ok(driver);
    }

    @PutMapping("/{driverId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DRIVER')")
    @Operation(summary = "Update driver details", description = "Updates phone, licence, or availability status for a driver")
    @ApiResponse(responseCode = "200", description = "Driver updated successfully")
    @ApiResponse(responseCode = "404", description = "Driver not found")
    public ResponseEntity<DriverResponse> updateDriver(
        @PathVariable UUID driverId,
        @Valid @RequestBody UpdateDriverRequest request
    ) {
        DriverResponse response = driverService.updateDriver(driverId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{driverId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete driver profile", description = "Soft-deletes a driver record from the platform")
    @ApiResponse(responseCode = "204", description = "Driver profile soft-deleted")
    @ApiResponse(responseCode = "404", description = "Driver not found")
    public ResponseEntity<Void> deleteDriver(@PathVariable UUID driverId) {
        driverService.deleteDriver(driverId);
        return ResponseEntity.noContent().build();
    }
}
