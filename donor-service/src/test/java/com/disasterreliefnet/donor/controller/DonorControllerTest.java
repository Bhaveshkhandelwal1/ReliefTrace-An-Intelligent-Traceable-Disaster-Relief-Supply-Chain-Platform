package com.disasterreliefnet.donor.controller;

import com.disasterreliefnet.common.enums.DonorStatus;
import com.disasterreliefnet.common.enums.DonorType;
import com.disasterreliefnet.common.exception.GlobalExceptionHandler;
import com.disasterreliefnet.common.exception.ResourceNotFoundException;
import com.disasterreliefnet.donor.client.dto.VerificationStatusDto;
import com.disasterreliefnet.donor.config.SecurityConfig;
import com.disasterreliefnet.donor.dto.*;
import com.disasterreliefnet.donor.service.DonorService;
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

@WebMvcTest(DonorController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class DonorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DonorService donorService;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    @DisplayName("POST /api/v1/donors should be publicly accessible and return 201 Created")
    void shouldCreateDonorPublicly() throws Exception {
        CreateDonorRequest request = new CreateDonorRequest(
            "Maria",
            "Santos",
            "maria@community.org",
            "+14155550199",
            DonorType.INDIVIDUAL,
            new AddressDto("10 Sunset Blvd", "San Jose", "CA", "US", "95112"),
            null
        );

        UUID donorId = UUID.randomUUID();
        DonorResponse response = new DonorResponse(
            donorId,
            request.firstName(),
            request.lastName(),
            request.email(),
            request.phone(),
            request.donorType(),
            request.address(),
            null,
            DonorStatus.ACTIVE,
            BigDecimal.ZERO,
            0L,
            Instant.now(),
            Instant.now()
        );

        when(donorService.createDonor(any(CreateDonorRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/donors")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.donorId").value(donorId.toString()))
            .andExpect(jsonPath("$.email").value("maria@community.org"))
            .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("GET /api/v1/donors/{id} as admin should return 200 OK")
    void shouldGetDonorByIdWhenAdmin() throws Exception {
        UUID donorId = UUID.randomUUID();
        DonorResponse response = new DonorResponse(
            donorId,
            "John",
            "Doe",
            "john@doe.com",
            null,
            DonorType.INDIVIDUAL,
            null,
            null,
            DonorStatus.ACTIVE,
            new BigDecimal("100.00"),
            1L,
            Instant.now(),
            Instant.now()
        );

        when(donorService.getDonorById(donorId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/donors/{donorId}", donorId)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.donorId").value(donorId.toString()))
            .andExpect(jsonPath("$.email").value("john@doe.com"));
    }

    @Test
    @DisplayName("GET /api/v1/donors/{id} as own donor should return 200 OK")
    void shouldGetDonorByIdWhenOwnDonor() throws Exception {
        UUID donorId = UUID.randomUUID();
        DonorResponse response = new DonorResponse(
            donorId,
            "John",
            "Doe",
            "john@doe.com",
            null,
            DonorType.INDIVIDUAL,
            null,
            null,
            DonorStatus.ACTIVE,
            BigDecimal.ZERO,
            0L,
            Instant.now(),
            Instant.now()
        );

        when(donorService.getDonorById(donorId)).thenReturn(response);

        // JWT subject matches donorId
        mockMvc.perform(get("/api/v1/donors/{donorId}", donorId)
                .with(jwt().jwt(j -> j.subject(donorId.toString()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.donorId").value(donorId.toString()));
    }

    @Test
    @DisplayName("GET /api/v1/donors/{id} when missing returns 404 ProblemDetail")
    void shouldReturn404WhenDonorNotFound() throws Exception {
        UUID donorId = UUID.randomUUID();
        when(donorService.getDonorById(donorId))
            .thenThrow(new ResourceNotFoundException("Donor not found with id: " + donorId));

        mockMvc.perform(get("/api/v1/donors/{donorId}", donorId)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    @DisplayName("GET /api/v1/donors/{id}/impact should return impact summary")
    void shouldGetDonorImpact() throws Exception {
        UUID donorId = UUID.randomUUID();
        DonorImpactSummaryDto impact = new DonorImpactSummaryDto(
            donorId,
            "Acme Health",
            DonorType.PHARMACY,
            5L,
            new BigDecimal("20000.00"),
            18L,
            Instant.now()
        );

        when(donorService.getDonorImpactSummary(donorId)).thenReturn(impact);

        mockMvc.perform(get("/api/v1/donors/{donorId}/impact", donorId)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_COORDINATOR"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.donorName").value("Acme Health"))
            .andExpect(jsonPath("$.impactDeliveriesCount").value(18));
    }
}
