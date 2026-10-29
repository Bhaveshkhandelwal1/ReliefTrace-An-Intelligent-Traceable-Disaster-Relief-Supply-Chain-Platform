package com.disasterreliefnet.disaster.service;

import com.disasterreliefnet.common.enums.DisasterStatus;
import com.disasterreliefnet.common.enums.DisasterType;
import com.disasterreliefnet.common.enums.SeverityLevel;
import com.disasterreliefnet.common.event.KafkaTopics;
import com.disasterreliefnet.common.exception.BusinessRuleException;
import com.disasterreliefnet.common.exception.ResourceNotFoundException;
import com.disasterreliefnet.common.outbox.OutboxService;
import com.disasterreliefnet.disaster.domain.Disaster;
import com.disasterreliefnet.disaster.domain.DisasterUpdate;
import com.disasterreliefnet.disaster.dto.*;
import com.disasterreliefnet.disaster.mapper.DisasterMapper;
import com.disasterreliefnet.disaster.mapper.DisasterMapperImpl;
import com.disasterreliefnet.disaster.mapper.DisasterUpdateMapper;
import com.disasterreliefnet.disaster.mapper.DisasterUpdateMapperImpl;
import com.disasterreliefnet.disaster.repository.DisasterRepository;
import com.disasterreliefnet.disaster.repository.DisasterUpdateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DisasterServiceTest {

    @Mock
    private DisasterRepository disasterRepository;

    @Mock
    private DisasterUpdateRepository disasterUpdateRepository;

    @Mock
    private OutboxService outboxService;

    private final DisasterMapper disasterMapper = new DisasterMapperImpl();
    private final DisasterUpdateMapper disasterUpdateMapper = new DisasterUpdateMapperImpl();

    private DisasterService disasterService;

    @BeforeEach
    void setUp() {
        disasterService = new DisasterService(
            disasterRepository,
            disasterUpdateRepository,
            disasterMapper,
            disasterUpdateMapper,
            outboxService
        );
    }

    @Test
    @DisplayName("Should create disaster and enqueue DisasterDeclared outbox event")
    void shouldCreateDisasterAndEnqueueOutboxEvent() {
        GeoAreaDto geoArea = new GeoAreaDto(19.0760, 72.8777, 50.0, "Mumbai", "IN");
        Instant now = Instant.now();
        CreateDisasterRequest request = new CreateDisasterRequest(
            "Monsoon Flood 2026",
            DisasterType.FLOOD,
            SeverityLevel.SEVERE,
            "Severe urban flooding in coastal areas",
            geoArea,
            now
        );

        UUID disasterId = UUID.randomUUID();
        when(disasterRepository.save(any(Disaster.class))).thenAnswer(invocation -> {
            Disaster d = invocation.getArgument(0);
            d.setDisasterId(disasterId);
            d.setStatus(DisasterStatus.ACTIVE);
            d.setCreatedAt(now);
            d.setUpdatedAt(now);
            return d;
        });

        DisasterResponse response = disasterService.createDisaster(request, "coordinator_user");

        assertNotNull(response);
        assertEquals(disasterId, response.disasterId());
        assertEquals("Monsoon Flood 2026", response.name());
        assertEquals(DisasterStatus.ACTIVE, response.status());
        assertEquals("IN", response.affectedArea().country());

        verify(outboxService).enqueue(
            eq(KafkaTopics.DISASTER_DECLARED),
            eq("DISASTER"),
            eq(disasterId),
            eq("DisasterDeclared"),
            any(),
            isNull()
        );
    }

    @Test
    @DisplayName("Should get disaster by ID")
    void shouldGetDisasterById() {
        UUID disasterId = UUID.randomUUID();
        Disaster disaster = Disaster.builder()
            .disasterId(disasterId)
            .name("Earthquake North")
            .type(DisasterType.EARTHQUAKE)
            .severityLevel(SeverityLevel.CATASTROPHIC)
            .latitude(28.7041)
            .longitude(77.1025)
            .radiusKm(100.0)
            .regionName("Delhi NCR")
            .country("IN")
            .status(DisasterStatus.ACTIVE)
            .occurredAt(Instant.now())
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        when(disasterRepository.findByDisasterIdAndDeletedAtIsNull(disasterId))
            .thenReturn(Optional.of(disaster));

        DisasterResponse response = disasterService.getDisasterById(disasterId);

        assertNotNull(response);
        assertEquals(disasterId, response.disasterId());
        assertEquals("Earthquake North", response.name());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when disaster not found")
    void shouldThrowNotFoundWhenDisasterMissing() {
        UUID disasterId = UUID.randomUUID();
        when(disasterRepository.findByDisasterIdAndDeletedAtIsNull(disasterId))
            .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> disasterService.getDisasterById(disasterId));
    }

    @Test
    @DisplayName("Should list disasters filtered by status and type")
    void shouldListDisastersWithFilters() {
        Pageable pageable = PageRequest.of(0, 10);
        Disaster d = Disaster.builder()
            .disasterId(UUID.randomUUID())
            .name("Wildfire Ridge")
            .type(DisasterType.WILDFIRE)
            .status(DisasterStatus.ACTIVE)
            .latitude(34.05)
            .longitude(-118.25)
            .occurredAt(Instant.now())
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        when(disasterRepository.findByStatusAndTypeAndDeletedAtIsNull(DisasterStatus.ACTIVE, DisasterType.WILDFIRE, pageable))
            .thenReturn(new PageImpl<>(List.of(d)));

        Page<DisasterResponse> result = disasterService.getDisasters(DisasterStatus.ACTIVE, DisasterType.WILDFIRE, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("Wildfire Ridge", result.getContent().get(0).name());
    }

    @Test
    @DisplayName("Should update active disaster and emit DisasterUpdated event")
    void shouldUpdateDisasterSuccessfully() {
        UUID disasterId = UUID.randomUUID();
        Disaster disaster = Disaster.builder()
            .disasterId(disasterId)
            .name("Cyclone Vayu")
            .type(DisasterType.HURRICANE)
            .severityLevel(SeverityLevel.MODERATE)
            .status(DisasterStatus.ACTIVE)
            .latitude(20.0)
            .longitude(70.0)
            .estimatedAffectedPeople(5000)
            .occurredAt(Instant.now())
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        when(disasterRepository.findByDisasterIdAndDeletedAtIsNull(disasterId))
            .thenReturn(Optional.of(disaster));
        when(disasterRepository.save(any(Disaster.class))).thenAnswer(i -> i.getArgument(0));

        UpdateDisasterRequest request = new UpdateDisasterRequest(
            SeverityLevel.SEVERE,
            "Wind speeds increased to 150 km/h",
            new GeoAreaDto(20.5, 70.8, 120.0, "Gujarat Coast", "IN"),
            25000L
        );

        DisasterResponse response = disasterService.updateDisaster(disasterId, request, "coord1");

        assertNotNull(response);
        assertEquals(SeverityLevel.SEVERE, response.severityLevel());
        assertEquals(25000L, response.estimatedAffectedPeople());
        assertEquals("Wind speeds increased to 150 km/h", response.description());

        verify(outboxService).enqueue(
            eq(KafkaTopics.DISASTER_UPDATED),
            eq("DISASTER"),
            eq(disasterId),
            eq("DisasterUpdated"),
            any(),
            isNull()
        );
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when updating closed disaster")
    void shouldThrowWhenUpdatingClosedDisaster() {
        UUID disasterId = UUID.randomUUID();
        Disaster disaster = Disaster.builder()
            .disasterId(disasterId)
            .status(DisasterStatus.CLOSED)
            .latitude(10.0)
            .longitude(20.0)
            .occurredAt(Instant.now())
            .build();

        when(disasterRepository.findByDisasterIdAndDeletedAtIsNull(disasterId))
            .thenReturn(Optional.of(disaster));

        UpdateDisasterRequest request = new UpdateDisasterRequest(SeverityLevel.MINOR, null, null, null);

        assertThrows(BusinessRuleException.class, () -> disasterService.updateDisaster(disasterId, request, "admin"));
    }

    @Test
    @DisplayName("Should close active disaster and emit DisasterClosed event")
    void shouldCloseDisasterSuccessfully() {
        UUID disasterId = UUID.randomUUID();
        Disaster disaster = Disaster.builder()
            .disasterId(disasterId)
            .name("Flood Zone 4")
            .type(DisasterType.FLOOD)
            .status(DisasterStatus.ACTIVE)
            .latitude(12.0)
            .longitude(77.0)
            .occurredAt(Instant.now())
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        when(disasterRepository.findByDisasterIdAndDeletedAtIsNull(disasterId))
            .thenReturn(Optional.of(disaster));
        when(disasterRepository.save(any(Disaster.class))).thenAnswer(i -> i.getArgument(0));

        CloseDisasterRequest request = new CloseDisasterRequest("Water receded, relief phase concluded");
        DisasterResponse response = disasterService.closeDisaster(disasterId, request, "admin_user");

        assertNotNull(response);
        assertEquals(DisasterStatus.CLOSED, response.status());

        verify(outboxService).enqueue(
            eq(KafkaTopics.DISASTER_CLOSED),
            eq("DISASTER"),
            eq(disasterId),
            eq("DisasterClosed"),
            any(),
            isNull()
        );
    }

    @Test
    @DisplayName("Should add situational update for active disaster")
    void shouldAddDisasterUpdateSuccessfully() {
        UUID disasterId = UUID.randomUUID();
        Disaster disaster = Disaster.builder()
            .disasterId(disasterId)
            .status(DisasterStatus.ACTIVE)
            .latitude(12.0)
            .longitude(77.0)
            .occurredAt(Instant.now())
            .build();

        when(disasterRepository.findByDisasterIdAndDeletedAtIsNull(disasterId))
            .thenReturn(Optional.of(disaster));

        UUID updateId = UUID.randomUUID();
        Instant now = Instant.now();
        when(disasterUpdateRepository.save(any(DisasterUpdate.class))).thenAnswer(invocation -> {
            DisasterUpdate u = invocation.getArgument(0);
            u.setUpdateId(updateId);
            u.setCreatedAt(now);
            return u;
        });

        CreateDisasterUpdateRequest request = new CreateDisasterUpdateRequest(
            "Evacuation in progress",
            "5,000 residents moved to temporary shelters",
            "Emergency Operation Center"
        );

        DisasterUpdateResponse response = disasterService.addDisasterUpdate(disasterId, request, "coordinator1");

        assertNotNull(response);
        assertEquals(updateId, response.updateId());
        assertEquals(disasterId, response.disasterId());
        assertEquals("Evacuation in progress", response.title());
        assertEquals("coordinator1", response.postedBy());
    }

    @Test
    @DisplayName("Should get situational updates for disaster")
    void shouldGetDisasterUpdates() {
        UUID disasterId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        Disaster disaster = Disaster.builder().disasterId(disasterId).build();

        DisasterUpdate update = DisasterUpdate.builder()
            .updateId(UUID.randomUUID())
            .disaster(disaster)
            .title("Update 1")
            .content("Content 1")
            .postedBy("admin")
            .createdAt(Instant.now())
            .build();

        when(disasterRepository.existsById(disasterId)).thenReturn(true);
        when(disasterUpdateRepository.findByDisaster_DisasterIdOrderByCreatedAtDesc(disasterId, pageable))
            .thenReturn(new PageImpl<>(List.of(update)));

        Page<DisasterUpdateResponse> page = disasterService.getDisasterUpdates(disasterId, pageable);

        assertEquals(1, page.getTotalElements());
        assertEquals("Update 1", page.getContent().get(0).title());
    }
}
