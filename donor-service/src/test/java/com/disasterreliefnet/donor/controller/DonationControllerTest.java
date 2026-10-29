package com.disasterreliefnet.donor.controller;

import com.disasterreliefnet.common.enums.DonationStatus;
import com.disasterreliefnet.common.enums.DonationType;
import com.disasterreliefnet.common.exception.GlobalExceptionHandler;
import com.disasterreliefnet.donor.config.SecurityConfig;
import com.disasterreliefnet.donor.dto.*;
import com.disasterreliefnet.donor.service.DonationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DonationController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class DonationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DonationService donationService;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    @DisplayName("POST /api/v1/donors/{donorId}/donations as own donor should return 201 Created")
    void shouldCreateDonation() throws Exception {
        UUID donorId = UUID.randomUUID();
        CreateDonationRequest request = new CreateDonationRequest(
            DonationType.MONETARY,
            new BigDecimal("300.00"),
            "USD",
            null,
            null,
            null,
            "For emergency relief",
            null
        );

        UUID donationId = UUID.randomUUID();
        DonationResponse response = new DonationResponse(
            donationId,
            donorId,
            request.donationType(),
            request.amount(),
            request.currency(),
            null,
            null,
            null,
            DonationStatus.PENDING,
            request.notes(),
            null,
            null,
            List.of(),
            Instant.now(),
            Instant.now()
        );

        when(donationService.createDonation(eq(donorId), any(CreateDonationRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/donors/{donorId}/donations", donorId)
                .with(jwt().jwt(j -> j.subject(donorId.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.donationId").value(donationId.toString()))
            .andExpect(jsonPath("$.amount").value(300.00))
            .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("PATCH /api/v1/donors/{donorId}/donations/{donationId}/cancel should cancel pledge")
    void shouldCancelDonation() throws Exception {
        UUID donorId = UUID.randomUUID();
        UUID donationId = UUID.randomUUID();

        DonationResponse response = new DonationResponse(
            donationId,
            donorId,
            DonationType.IN_KIND,
            null,
            null,
            "Clothing",
            null,
            null,
            DonationStatus.CANCELLED,
            null,
            null,
            null,
            List.of(),
            Instant.now(),
            Instant.now()
        );

        when(donationService.cancelDonation(donorId, donationId)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/donors/{donorId}/donations/{donationId}/cancel", donorId, donationId)
                .with(jwt().jwt(j -> j.subject(donorId.toString()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("GET /api/v1/donors/{donorId}/donations/{donationId}/receipt should return tax receipt")
    void shouldGetReceipt() throws Exception {
        UUID donorId = UUID.randomUUID();
        UUID donationId = UUID.randomUUID();

        DonationReceiptDto receipt = new DonationReceiptDto(
            "REC-98765432",
            donationId,
            donorId,
            "Bruce Wayne",
            "bruce@wayne.com",
            DonationType.MONETARY,
            new BigDecimal("5000.00"),
            "USD",
            null,
            Instant.now(),
            true
        );

        when(donationService.getDonationReceipt(donorId, donationId)).thenReturn(receipt);

        mockMvc.perform(get("/api/v1/donors/{donorId}/donations/{donationId}/receipt", donorId, donationId)
                .with(jwt().jwt(j -> j.subject(donorId.toString()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.receiptNumber").value("REC-98765432"))
            .andExpect(jsonPath("$.taxDeductible").value(true));
    }

    @Test
    @DisplayName("PATCH /api/v1/donors/{donorId}/donations/{donationId}/status as COORDINATOR should transition status")
    void shouldTransitionStatus() throws Exception {
        UUID donorId = UUID.randomUUID();
        UUID donationId = UUID.randomUUID();

        DonationResponse response = new DonationResponse(
            donationId,
            donorId,
            DonationType.IN_KIND,
            null,
            null,
            "Medical masks",
            new BigDecimal("20.00"),
            null,
            DonationStatus.RECEIVED,
            "Verified at warehouse",
            "REC-11223344",
            Instant.now(),
            List.of(),
            Instant.now(),
            Instant.now()
        );

        when(donationService.transitionDonationStatus(eq(donationId), eq(DonationStatus.RECEIVED), any())).thenReturn(response);

        mockMvc.perform(patch("/api/v1/donors/{donorId}/donations/{donationId}/status", donorId, donationId)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_COORDINATOR")))
                .param("status", "RECEIVED")
                .param("notes", "Verified at warehouse"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("RECEIVED"));
    }
}
