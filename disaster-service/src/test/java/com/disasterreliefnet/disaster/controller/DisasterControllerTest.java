package com.disasterreliefnet.disaster.controller;

import com.disasterreliefnet.common.enums.DisasterStatus;
import com.disasterreliefnet.common.enums.DisasterType;
import com.disasterreliefnet.common.enums.SeverityLevel;
import com.disasterreliefnet.common.exception.GlobalExceptionHandler;
import com.disasterreliefnet.common.exception.ResourceNotFoundException;
import com.disasterreliefnet.disaster.config.SecurityConfig;
import com.disasterreliefnet.disaster.dto.*;
import com.disasterreliefnet.disaster.service.DisasterService;
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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DisasterController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class DisasterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DisasterService disasterService;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    @DisplayName("POST /api/v1/disasters with ROLE_COORDINATOR should return 201 Created")
    void shouldCreateDisasterWhenAuthorized() throws Exception {
        GeoAreaDto geoArea = new GeoAreaDto(12.9716, 77.5946, 30.0, "Bangalore South", "IN");
        CreateDisasterRequest request = new CreateDisasterRequest(
            "Urban Flash Flood",
            DisasterType.FLOOD,
            SeverityLevel.SEVERE,
            "Flooding across major corridors",
            geoArea,
            Instant.now()
        );

        UUID disasterId = UUID.randomUUID();
        DisasterResponse response = new DisasterResponse(
            disasterId,
            request.name(),
            request.type(),
            request.severityLevel(),
            request.description(),
            geoArea,
            0L,
            DisasterStatus.ACTIVE,
            request.occurredAt(),
            null,
            Instant.now(),
            Instant.now()
        );

        when(disasterService.createDisaster(any(CreateDisasterRequest.class), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/disasters")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_COORDINATOR")).jwt(j -> j.subject("coord-1")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.disasterId").value(disasterId.toString()))
            .andExpect(jsonPath("$.name").value("Urban Flash Flood"))
            .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("POST /api/v1/disasters without proper role should return 403 Forbidden")
    void shouldReturnForbiddenWhenRoleInvalid() throws Exception {
        GeoAreaDto geoArea = new GeoAreaDto(12.9716, 77.5946, 30.0, "Bangalore", "IN");
        CreateDisasterRequest request = new CreateDisasterRequest(
            "Flash Flood",
            DisasterType.FLOOD,
            SeverityLevel.SEVERE,
            "Description",
            geoArea,
            Instant.now()
        );

        mockMvc.perform(post("/api/v1/disasters")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DONOR")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/disasters/{id} should return 200 OK")
    void shouldGetDisasterById() throws Exception {
        UUID disasterId = UUID.randomUUID();
        GeoAreaDto geoArea = new GeoAreaDto(12.9716, 77.5946, 30.0, "Bangalore", "IN");
        DisasterResponse response = new DisasterResponse(
            disasterId,
            "Earthquake 5.8",
            DisasterType.EARTHQUAKE,
            SeverityLevel.MODERATE,
            "Tremors felt in suburbs",
            geoArea,
            1200L,
            DisasterStatus.ACTIVE,
            Instant.now(),
            null,
            Instant.now(),
            Instant.now()
        );

        when(disasterService.getDisasterById(disasterId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/disasters/{disasterId}", disasterId)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_RECEIVER"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.disasterId").value(disasterId.toString()))
            .andExpect(jsonPath("$.name").value("Earthquake 5.8"));
    }

    @Test
    @DisplayName("GET /api/v1/disasters/{id} when not found should return 404 ProblemDetail")
    void shouldReturn404WhenNotFound() throws Exception {
        UUID disasterId = UUID.randomUUID();
        when(disasterService.getDisasterById(disasterId))
            .thenThrow(new ResourceNotFoundException("Disaster not found with id: " + disasterId));

        mockMvc.perform(get("/api/v1/disasters/{disasterId}", disasterId)
                .with(jwt()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("Resource Not Found"))
            .andExpect(jsonPath("$.detail").value("Disaster not found with id: " + disasterId));
    }

    @Test
    @DisplayName("PATCH /api/v1/disasters/{id}/close with ROLE_ADMIN should return 200 OK")
    void shouldCloseDisasterWhenAdmin() throws Exception {
        UUID disasterId = UUID.randomUUID();
        CloseDisasterRequest request = new CloseDisasterRequest("Water receded, operations ceased");
        GeoAreaDto geoArea = new GeoAreaDto(10.0, 75.0, 20.0, "Coast", "IN");
        DisasterResponse response = new DisasterResponse(
            disasterId,
            "Closed Disaster",
            DisasterType.FLOOD,
            SeverityLevel.MINOR,
            "Receding",
            geoArea,
            0L,
            DisasterStatus.CLOSED,
            Instant.now(),
            Instant.now(),
            Instant.now(),
            Instant.now()
        );

        when(disasterService.closeDisaster(eq(disasterId), any(), any())).thenReturn(response);

        mockMvc.perform(patch("/api/v1/disasters/{disasterId}/close", disasterId)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")).jwt(j -> j.subject("admin-user")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CLOSED"));
    }
}
