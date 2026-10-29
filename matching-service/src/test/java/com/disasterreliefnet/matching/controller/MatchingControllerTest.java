package com.disasterreliefnet.matching.controller;

import com.disasterreliefnet.common.enums.AllocationStatus;
import com.disasterreliefnet.common.exception.GlobalExceptionHandler;
import com.disasterreliefnet.matching.config.SecurityConfig;
import com.disasterreliefnet.matching.dto.*;
import com.disasterreliefnet.matching.exception.AllocationNotFoundException;
import com.disasterreliefnet.matching.service.MatchingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MatchingController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class MatchingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MatchingService matchingService;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    @DisplayName("GET /api/v1/matching/suggestions with COORDINATOR role should return 200")
    void shouldGetSuggestionsWithCoordinatorRole() throws Exception {
        UUID reqId = UUID.randomUUID();
        AllocationSuggestionResponse suggestion = new AllocationSuggestionResponse(
            reqId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Family Food Pack",
            new BigDecimal("100.000"),
            0.95,
            "Optimal match"
        );

        when(matchingService.generateSuggestions(reqId)).thenReturn(List.of(suggestion));

        mockMvc.perform(get("/api/v1/matching/suggestions")
                .param("requestId", reqId.toString())
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_COORDINATOR"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].itemName").value("Family Food Pack"))
            .andExpect(jsonPath("$[0].matchScore").value(0.95));
    }

    @Test
    @DisplayName("POST /api/v1/matching/allocations should propose allocation and return 201")
    void shouldCreateAllocation() throws Exception {
        UUID allocId = UUID.randomUUID();
        UUID reqId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        CreateAllocationRequest request = new CreateAllocationRequest(reqId, itemId, new BigDecimal("25.000"), "Immediate need");

        AllocationResponse response = new AllocationResponse(
            allocId,
            reqId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            itemId,
            UUID.randomUUID(),
            new BigDecimal("25.000"),
            "boxes",
            AllocationStatus.PENDING_APPROVAL,
            null,
            "Immediate need",
            null,
            80.0,
            Instant.now(),
            Instant.now()
        );

        when(matchingService.createAllocation(any(CreateAllocationRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/matching/allocations")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_COORDINATOR")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.allocationId").value(allocId.toString()))
            .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"));
    }

    @Test
    @DisplayName("GET /api/v1/matching/allocations/{allocationId} should return 404 when allocation not found")
    void shouldReturn404WhenAllocationNotFound() throws Exception {
        UUID allocId = UUID.randomUUID();
        when(matchingService.getAllocation(allocId)).thenThrow(new AllocationNotFoundException(allocId));

        mockMvc.perform(get("/api/v1/matching/allocations/{allocationId}", allocId)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_COORDINATOR"))))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    @DisplayName("PATCH /api/v1/matching/allocations/{allocationId}/approve should approve allocation")
    void shouldApproveAllocation() throws Exception {
        UUID allocId = UUID.randomUUID();
        ApproveAllocationRequest request = new ApproveAllocationRequest("coordinator-1", "Approved for dispatch");

        AllocationResponse response = new AllocationResponse(
            allocId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            new BigDecimal("20.000"),
            "boxes",
            AllocationStatus.APPROVED,
            "coordinator-1",
            "Approved for dispatch",
            null,
            85.0,
            Instant.now(),
            Instant.now()
        );

        when(matchingService.approveAllocation(eq(allocId), any(ApproveAllocationRequest.class), anyString()))
            .thenReturn(response);

        mockMvc.perform(patch("/api/v1/matching/allocations/{allocationId}/approve", allocId)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_COORDINATOR")).jwt(j -> j.subject("coord-1")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("APPROVED"))
            .andExpect(jsonPath("$.approvedBy").value("coordinator-1"));
    }

    @Test
    @DisplayName("PATCH /api/v1/matching/allocations/{allocationId}/reject with empty reason should return 400")
    void shouldReturn400WhenRejectionReasonBlank() throws Exception {
        UUID allocId = UUID.randomUUID();
        RejectAllocationRequest request = new RejectAllocationRequest("");

        mockMvc.perform(patch("/api/v1/matching/allocations/{allocationId}/reject", allocId)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_COORDINATOR")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }
}
