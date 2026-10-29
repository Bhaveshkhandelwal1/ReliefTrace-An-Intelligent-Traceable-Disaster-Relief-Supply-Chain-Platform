package com.disasterreliefnet.receiver.controller;

import com.disasterreliefnet.common.dto.DeliveryAddressDto;
import com.disasterreliefnet.common.enums.AidCategory;
import com.disasterreliefnet.common.enums.RequestStatus;
import com.disasterreliefnet.common.enums.UrgencyLevel;
import com.disasterreliefnet.common.enums.VerificationStatus;
import com.disasterreliefnet.common.exception.GlobalExceptionHandler;
import com.disasterreliefnet.receiver.config.SecurityConfig;
import com.disasterreliefnet.receiver.dto.*;
import com.disasterreliefnet.receiver.exception.ReceiverNotFoundException;
import com.disasterreliefnet.receiver.service.ReceiverService;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReceiverController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class ReceiverControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReceiverService receiverService;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    @DisplayName("POST /api/v1/receivers should be publicly accessible and return 201 Created")
    void shouldRegisterReceiverPublicly() throws Exception {
        UUID receiverId = UUID.randomUUID();
        UUID disasterId = UUID.randomUUID();
        DeliveryAddressDto address = new DeliveryAddressDto("10 Flood Way", "Rivertown", "North", "US", "98001");

        CreateReceiverRequest request = new CreateReceiverRequest(
            "Alice",
            "Smith",
            "alice@example.com",
            "+15550001122",
            disasterId,
            address,
            3,
            List.of("ELDERLY")
        );

        ReceiverResponse response = new ReceiverResponse(
            receiverId,
            "Alice",
            "Smith",
            "alice@example.com",
            "+15550001122",
            disasterId,
            address,
            3,
            List.of("ELDERLY"),
            VerificationStatus.UNVERIFIED,
            Instant.now(),
            Instant.now()
        );

        when(receiverService.registerReceiver(any(CreateReceiverRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/receivers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.receiverId").value(receiverId.toString()))
            .andExpect(jsonPath("$.firstName").value("Alice"))
            .andExpect(jsonPath("$.verificationStatus").value("UNVERIFIED"));
    }

    @Test
    @DisplayName("GET /api/v1/receivers/{receiverId} should allow COORDINATOR role")
    void shouldGetReceiverWithCoordinatorRole() throws Exception {
        UUID receiverId = UUID.randomUUID();
        UUID disasterId = UUID.randomUUID();
        DeliveryAddressDto address = new DeliveryAddressDto("10 Flood Way", "Rivertown", "North", "US", "98001");

        ReceiverResponse response = new ReceiverResponse(
            receiverId,
            "Alice",
            "Smith",
            "alice@example.com",
            "+15550001122",
            disasterId,
            address,
            3,
            List.of("ELDERLY"),
            VerificationStatus.UNVERIFIED,
            Instant.now(),
            Instant.now()
        );

        when(receiverService.getReceiver(receiverId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/receivers/{receiverId}", receiverId)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_COORDINATOR"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.receiverId").value(receiverId.toString()))
            .andExpect(jsonPath("$.lastName").value("Smith"));
    }

    @Test
    @DisplayName("GET /api/v1/receivers/{receiverId} should return 404 when receiver does not exist")
    void shouldReturn404WhenReceiverNotFound() throws Exception {
        UUID receiverId = UUID.randomUUID();
        when(receiverService.getReceiver(receiverId)).thenThrow(new ReceiverNotFoundException(receiverId));

        mockMvc.perform(get("/api/v1/receivers/{receiverId}", receiverId)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    @DisplayName("DELETE /api/v1/receivers/{receiverId} should require ADMIN role and return 204")
    void shouldDeleteReceiverWithAdminRole() throws Exception {
        UUID receiverId = UUID.randomUUID();
        doNothing().when(receiverService).deleteReceiver(receiverId);

        mockMvc.perform(delete("/api/v1/receivers/{receiverId}", receiverId)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /api/v1/receivers/{receiverId}/requests should create aid request and return 201")
    void shouldCreateAidRequest() throws Exception {
        UUID receiverId = UUID.randomUUID();
        UUID disasterId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        DeliveryAddressDto address = new DeliveryAddressDto("10 Flood Way", "Rivertown", "North", "US", "98001");

        CreateAidRequestRequest request = new CreateAidRequestRequest(
            disasterId,
            AidCategory.MEDICINE,
            "Insulin supplies",
            new BigDecimal("5.000"),
            "kits",
            UrgencyLevel.CRITICAL,
            address
        );

        AidRequestResponse response = new AidRequestResponse(
            requestId,
            receiverId,
            disasterId,
            AidCategory.MEDICINE,
            "Insulin supplies",
            new BigDecimal("5.000"),
            "kits",
            UrgencyLevel.CRITICAL,
            address,
            RequestStatus.PENDING,
            null,
            Instant.now(),
            Instant.now()
        );

        when(receiverService.createAidRequest(eq(receiverId), any(CreateAidRequestRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/receivers/{receiverId}/requests", receiverId)
                .with(jwt().jwt(builder -> builder.subject(receiverId.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.requestId").value(requestId.toString()))
            .andExpect(jsonPath("$.category").value("MEDICINE"))
            .andExpect(jsonPath("$.urgencyLevel").value("CRITICAL"));
    }

    @Test
    @DisplayName("PATCH /api/v1/receivers/{receiverId}/requests/{requestId}/cancel should cancel request")
    void shouldCancelAidRequest() throws Exception {
        UUID receiverId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        DeliveryAddressDto address = new DeliveryAddressDto("10 Flood Way", "Rivertown", "North", "US", "98001");

        AidRequestResponse response = new AidRequestResponse(
            requestId,
            receiverId,
            UUID.randomUUID(),
            AidCategory.FOOD,
            "Food",
            new BigDecimal("10.000"),
            "boxes",
            UrgencyLevel.MEDIUM,
            address,
            RequestStatus.CANCELLED,
            null,
            Instant.now(),
            Instant.now()
        );

        when(receiverService.cancelAidRequest(receiverId, requestId)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/receivers/{receiverId}/requests/{requestId}/cancel", receiverId, requestId)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}
