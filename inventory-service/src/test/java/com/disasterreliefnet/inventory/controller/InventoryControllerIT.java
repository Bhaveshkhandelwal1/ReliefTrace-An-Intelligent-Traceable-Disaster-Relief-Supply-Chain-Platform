package com.disasterreliefnet.inventory.controller;

import com.disasterreliefnet.common.enums.AidCategory;
import com.disasterreliefnet.inventory.dto.AddressDto;
import com.disasterreliefnet.inventory.dto.CreateWarehouseRequest;
import com.disasterreliefnet.inventory.dto.ItemResponse;
import com.disasterreliefnet.inventory.dto.WarehouseResponse;
import com.disasterreliefnet.inventory.service.ItemService;
import com.disasterreliefnet.inventory.service.ReservationService;
import com.disasterreliefnet.inventory.service.WarehouseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "eureka.client.enabled=false",
    "spring.cloud.discovery.enabled=false",
    "spring.datasource.url=jdbc:postgresql://localhost:5432/inventory_db"
})
@AutoConfigureMockMvc
class InventoryControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WarehouseService warehouseService;

    @MockBean
    private ItemService itemService;

    @MockBean
    private ReservationService reservationService;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void shouldDenyUnauthenticatedAccessToWarehouses() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/warehouses"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldDenyDonorAccessToWarehouses() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/warehouses")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DONOR"))))
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowCoordinatorAccessToWarehouses() throws Exception {
        when(warehouseService.getWarehouses(any()))
            .thenReturn(new PageImpl<>(List.of(
                new WarehouseResponse(
                    UUID.randomUUID(),
                    "Central Hub",
                    new AddressDto("123 St", "Miami", "FL", "US", "33101"),
                    new BigDecimal("1000"),
                    true,
                    Instant.now(),
                    Instant.now()
                )
            )));

        mockMvc.perform(get("/api/v1/inventory/warehouses")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_COORDINATOR"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].name").value("Central Hub"));
    }

    @Test
    void shouldAllowDriverToReadItems() throws Exception {
        when(itemService.getItems(any(), any(), any()))
            .thenReturn(new PageImpl<>(List.of(
                new ItemResponse(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "Blankets",
                    AidCategory.SHELTER,
                    "pieces",
                    new BigDecimal("100"),
                    new BigDecimal("20"),
                    new BigDecimal("80"),
                    null,
                    Instant.now(),
                    Instant.now()
                )
            )));

        mockMvc.perform(get("/api/v1/inventory/items")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DRIVER"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].name").value("Blankets"));
    }

    @Test
    void shouldDenyDriverToCreateWarehouse() throws Exception {
        CreateWarehouseRequest request = new CreateWarehouseRequest(
            "Driver Depot",
            new AddressDto("10 Road", "Dallas", "TX", "US", "75001"),
            new BigDecimal("500")
        );

        mockMvc.perform(post("/api/v1/inventory/warehouses")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DRIVER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }
}
