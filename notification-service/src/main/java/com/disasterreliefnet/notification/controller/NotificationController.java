package com.disasterreliefnet.notification.controller;

import com.disasterreliefnet.notification.dto.NotificationLogResponse;
import com.disasterreliefnet.notification.dto.NotificationPreferenceResponse;
import com.disasterreliefnet.notification.dto.UpdatePreferenceRequest;
import com.disasterreliefnet.notification.service.NotificationPreferenceService;
import com.disasterreliefnet.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "Endpoints for notification audit logs and user delivery channel preferences")
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationPreferenceService preferenceService;

    public NotificationController(
        NotificationService notificationService,
        NotificationPreferenceService preferenceService
    ) {
        this.notificationService = notificationService;
        this.preferenceService = preferenceService;
    }

    @GetMapping("/logs")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List notification audit logs", description = "Retrieves paginated dispatch logs, optionally filtered by user ID")
    @ApiResponse(responseCode = "200", description = "Paginated list of notification logs")
    public ResponseEntity<Page<NotificationLogResponse>> listLogs(
        @RequestParam(required = false) String userId,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<NotificationLogResponse> logs = notificationService.getLogs(userId, pageable);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/logs/{logId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get notification log entry", description = "Retrieves detailed log of a single notification event")
    @ApiResponse(responseCode = "200", description = "Notification log entry found")
    @ApiResponse(responseCode = "404", description = "Log not found")
    public ResponseEntity<NotificationLogResponse> getLogById(@PathVariable UUID logId) {
        NotificationLogResponse log = notificationService.getLogById(logId);
        return ResponseEntity.ok(log);
    }

    @GetMapping("/preferences/{userId}")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.name")
    @Operation(summary = "Get user notification preferences", description = "Retrieves channels enabled for a user")
    @ApiResponse(responseCode = "200", description = "Notification preferences found")
    public ResponseEntity<NotificationPreferenceResponse> getPreferences(@PathVariable String userId) {
        NotificationPreferenceResponse response = preferenceService.getPreferences(userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/preferences/{userId}")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.name")
    @Operation(summary = "Update user notification preferences", description = "Updates opt-in flags for Email, SMS, or Push channels")
    @ApiResponse(responseCode = "200", description = "Preferences updated successfully")
    public ResponseEntity<NotificationPreferenceResponse> updatePreferences(
        @PathVariable String userId,
        @Valid @RequestBody UpdatePreferenceRequest request
    ) {
        NotificationPreferenceResponse response = preferenceService.updatePreferences(userId, request);
        return ResponseEntity.ok(response);
    }
}
