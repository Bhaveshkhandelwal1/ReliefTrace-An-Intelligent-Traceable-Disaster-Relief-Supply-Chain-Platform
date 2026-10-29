package com.disasterreliefnet.inventory.controller;

import com.disasterreliefnet.inventory.dto.ReservationResponse;
import com.disasterreliefnet.inventory.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory/reservations")
@Tag(name = "Reservations", description = "Endpoints for viewing and releasing inventory reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    @Operation(summary = "List reservations", description = "Retrieves active and historical inventory reservations")
    public ResponseEntity<Page<ReservationResponse>> getReservations(
        @RequestParam(value = "allocationId", required = false) UUID allocationId,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(reservationService.getReservations(allocationId, pageable));
    }

    @GetMapping("/{reservationId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    @Operation(summary = "Get reservation by ID", description = "Retrieves status and details of a specific stock reservation")
    public ResponseEntity<ReservationResponse> getReservation(@PathVariable("reservationId") UUID reservationId) {
        return ResponseEntity.ok(reservationService.getReservation(reservationId));
    }

    @DeleteMapping("/{reservationId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    @Operation(summary = "Release reservation", description = "Manually cancels and releases reserved stock back to available pool")
    @ApiResponse(responseCode = "204", description = "Reservation released successfully")
    public ResponseEntity<Void> releaseReservation(@PathVariable("reservationId") UUID reservationId) {
        reservationService.releaseStock(reservationId, "Manually cancelled via API");
        return ResponseEntity.noContent().build();
    }
}
