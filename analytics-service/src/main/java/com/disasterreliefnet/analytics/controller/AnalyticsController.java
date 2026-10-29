package com.disasterreliefnet.analytics.controller;

import com.disasterreliefnet.analytics.dto.*;
import com.disasterreliefnet.analytics.service.AnalyticsQueryService;
import com.disasterreliefnet.common.enums.DisasterStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analytics")
@Tag(name = "Analytics", description = "Endpoints for platform KPIs, impact metrics, and situation dashboards")
public class AnalyticsController {

    private final AnalyticsQueryService queryService;

    public AnalyticsController(AnalyticsQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/donations/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    @Operation(summary = "Donation metrics summary", description = "Aggregates total pledges, monetary value, and relief weight")
    @ApiResponse(responseCode = "200", description = "Donation summary metrics")
    public ResponseEntity<DonationSummaryResponse> getDonationSummary(
        @RequestParam(required = false) UUID disasterId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate
    ) {
        DonationSummaryResponse response = queryService.getDonationSummary(disasterId, fromDate, toDate);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/requests/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    @Operation(summary = "Aid request pipeline summary", description = "Aggregates request volumes across pending, verified, allocated, and delivered stages")
    @ApiResponse(responseCode = "200", description = "Request summary metrics")
    public ResponseEntity<RequestSummaryResponse> getRequestSummary(
        @RequestParam(required = false) UUID disasterId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate
    ) {
        RequestSummaryResponse response = queryService.getRequestSummary(disasterId, fromDate, toDate);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/shipments/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    @Operation(summary = "Logistics throughput summary", description = "Aggregates shipment dispatch volume, in-transit runs, disputes, and delivery times")
    @ApiResponse(responseCode = "200", description = "Shipment summary metrics")
    public ResponseEntity<ShipmentSummaryResponse> getShipmentSummary(
        @RequestParam(required = false) UUID disasterId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate
    ) {
        ShipmentSummaryResponse response = queryService.getShipmentSummary(disasterId, fromDate, toDate);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/disasters/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    @Operation(summary = "Disaster incident summaries", description = "Lists high-level situation reports and operational statistics per disaster")
    @ApiResponse(responseCode = "200", description = "List of disaster situation summaries")
    public ResponseEntity<List<DisasterSummaryResponse>> getDisastersSummary(
        @RequestParam(required = false) DisasterStatus status
    ) {
        List<DisasterSummaryResponse> response = queryService.getDisastersSummary(status);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    @Operation(summary = "Executive situational dashboard", description = "Consolidated dashboard view covering donations, requests, transport, and active incidents")
    @ApiResponse(responseCode = "200", description = "Executive dashboard view")
    public ResponseEntity<DashboardResponse> getDashboard(
        @RequestParam(required = false) UUID disasterId
    ) {
        DashboardResponse response = queryService.getDashboard(disasterId);
        return ResponseEntity.ok(response);
    }
}
