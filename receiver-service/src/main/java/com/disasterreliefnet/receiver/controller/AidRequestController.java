package com.disasterreliefnet.receiver.controller;

import com.disasterreliefnet.common.enums.RequestStatus;
import com.disasterreliefnet.receiver.dto.AidRequestResponse;
import com.disasterreliefnet.receiver.service.ReceiverService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/requests")
@Tag(name = "Aid Requests", description = "Cross-cutting endpoints for querying and filtering relief aid requests")
public class AidRequestController {

    private final ReceiverService receiverService;

    public AidRequestController(ReceiverService receiverService) {
        this.receiverService = receiverService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    @Operation(summary = "List all aid requests", description = "Cross-cutting listing for relief coordinators with optional disasterId and status filters")
    @ApiResponse(responseCode = "200", description = "Paginated list of aid requests")
    public ResponseEntity<Page<AidRequestResponse>> listAllRequests(
        @RequestParam(required = false) UUID disasterId,
        @RequestParam(required = false) RequestStatus status,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<AidRequestResponse> page = receiverService.listAllRequests(disasterId, status, pageable);
        return ResponseEntity.ok(page);
    }
}
