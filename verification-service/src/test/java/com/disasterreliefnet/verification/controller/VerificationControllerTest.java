package com.disasterreliefnet.verification.controller;

import com.disasterreliefnet.common.enums.CaseStatus;
import com.disasterreliefnet.common.enums.DocumentType;
import com.disasterreliefnet.common.exception.GlobalExceptionHandler;
import com.disasterreliefnet.verification.config.SecurityConfig;
import com.disasterreliefnet.verification.dto.*;
import com.disasterreliefnet.verification.exception.VerificationCaseNotFoundException;
import com.disasterreliefnet.verification.service.VerificationService;
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

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VerificationController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class VerificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private VerificationService verificationService;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    @DisplayName("GET /api/v1/verification/cases/{caseId} with COORDINATOR role should return 200")
    void shouldGetCaseWithCoordinatorRole() throws Exception {
        UUID caseId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();

        VerificationCaseResponse response = new VerificationCaseResponse(
            caseId,
            requestId,
            receiverId,
            CaseStatus.OPEN,
            Collections.emptyList(),
            null,
            null,
            Instant.now(),
            null,
            Instant.now(),
            Instant.now()
        );

        when(verificationService.getCase(caseId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/verification/cases/{caseId}", caseId)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_COORDINATOR"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.caseId").value(caseId.toString()))
            .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    @DisplayName("GET /api/v1/verification/cases/{caseId} should return 404 when case not found")
    void shouldReturn404WhenNotFound() throws Exception {
        UUID caseId = UUID.randomUUID();
        when(verificationService.getCase(caseId)).thenThrow(new VerificationCaseNotFoundException(caseId));

        mockMvc.perform(get("/api/v1/verification/cases/{caseId}", caseId)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    @DisplayName("PATCH /api/v1/verification/cases/{caseId}/approve should approve case and return 200")
    void shouldApproveCase() throws Exception {
        UUID caseId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();

        ApproveVerificationRequest request = new ApproveVerificationRequest("Approved all documents");
        VerificationCaseResponse response = new VerificationCaseResponse(
            caseId,
            requestId,
            receiverId,
            CaseStatus.APPROVED,
            Collections.emptyList(),
            "coordinator-user",
            "Approved all documents",
            Instant.now(),
            Instant.now(),
            Instant.now(),
            Instant.now()
        );

        when(verificationService.approveCase(eq(caseId), any(ApproveVerificationRequest.class), anyString()))
            .thenReturn(response);

        mockMvc.perform(patch("/api/v1/verification/cases/{caseId}/approve", caseId)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_COORDINATOR")).jwt(j -> j.subject("coord-1")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("APPROVED"))
            .andExpect(jsonPath("$.reviewedBy").value("coordinator-user"));
    }

    @Test
    @DisplayName("PATCH /api/v1/verification/cases/{caseId}/reject with blank notes should return 400")
    void shouldReturn400WhenRejectNotesBlank() throws Exception {
        UUID caseId = UUID.randomUUID();
        RejectVerificationRequest request = new RejectVerificationRequest("");

        mockMvc.perform(patch("/api/v1/verification/cases/{caseId}/reject", caseId)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_COORDINATOR")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/verification/cases/{caseId}/documents should allow RECEIVER role")
    void shouldUploadDocumentWithReceiverRole() throws Exception {
        UUID caseId = UUID.randomUUID();
        UploadDocumentRequest request = new UploadDocumentRequest(
            DocumentType.PROOF_OF_ADDRESS,
            "https://cdn.relief.org/docs/address.jpg"
        );

        DocumentDto docDto = new DocumentDto(UUID.randomUUID(), DocumentType.PROOF_OF_ADDRESS, "https://cdn.relief.org/docs/address.jpg", Instant.now());
        VerificationCaseResponse response = new VerificationCaseResponse(
            caseId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            CaseStatus.UNDER_REVIEW,
            List.of(docDto),
            null,
            null,
            Instant.now(),
            null,
            Instant.now(),
            Instant.now()
        );

        when(verificationService.uploadDocument(eq(caseId), any(UploadDocumentRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/verification/cases/{caseId}/documents", caseId)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_RECEIVER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UNDER_REVIEW"))
            .andExpect(jsonPath("$.documents[0].documentType").value("PROOF_OF_ADDRESS"));
    }
}
