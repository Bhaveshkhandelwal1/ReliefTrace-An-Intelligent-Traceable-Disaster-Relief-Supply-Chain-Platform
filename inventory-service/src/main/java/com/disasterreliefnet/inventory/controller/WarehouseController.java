package com.disasterreliefnet.inventory.controller;

import com.disasterreliefnet.inventory.dto.CreateWarehouseRequest;
import com.disasterreliefnet.inventory.dto.UpdateWarehouseRequest;
import com.disasterreliefnet.inventory.dto.WarehouseResponse;
import com.disasterreliefnet.inventory.service.WarehouseService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory/warehouses")
@Tag(name = "Warehouses", description = "Endpoints for managing storage warehouse facilities")
public class WarehouseController {

    private final WarehouseService warehouseService;

    public WarehouseController(WarehouseService warehouseService) {
        this.warehouseService = warehouseService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    @Operation(summary = "Create a warehouse", description = "Registers a new storage facility with address and capacity")
    @ApiResponse(responseCode = "201", description = "Warehouse registered successfully")
    public ResponseEntity<WarehouseResponse> createWarehouse(@Valid @RequestBody CreateWarehouseRequest request) {
        WarehouseResponse response = warehouseService.createWarehouse(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    @Operation(summary = "List warehouses", description = "Retrieves a paginated list of active warehouses")
    public ResponseEntity<Page<WarehouseResponse>> getWarehouses(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(warehouseService.getWarehouses(pageable));
    }

    @GetMapping("/{warehouseId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    @Operation(summary = "Get warehouse by ID", description = "Retrieves full details of a specific warehouse")
    public ResponseEntity<WarehouseResponse> getWarehouse(@PathVariable("warehouseId") UUID warehouseId) {
        return ResponseEntity.ok(warehouseService.getWarehouse(warehouseId));
    }

    @PutMapping("/{warehouseId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    @Operation(summary = "Update warehouse", description = "Updates warehouse address or capacity")
    public ResponseEntity<WarehouseResponse> updateWarehouse(
        @PathVariable("warehouseId") UUID warehouseId,
        @Valid @RequestBody UpdateWarehouseRequest request
    ) {
        return ResponseEntity.ok(warehouseService.updateWarehouse(warehouseId, request));
    }
}
