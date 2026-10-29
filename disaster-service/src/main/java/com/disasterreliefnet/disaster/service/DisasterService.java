package com.disasterreliefnet.disaster.service;

import com.disasterreliefnet.common.dto.AffectedAreaDto;
import com.disasterreliefnet.common.enums.DisasterStatus;
import com.disasterreliefnet.common.enums.DisasterType;
import com.disasterreliefnet.common.event.KafkaTopics;
import com.disasterreliefnet.common.event.payload.DisasterClosedPayload;
import com.disasterreliefnet.common.event.payload.DisasterDeclaredPayload;
import com.disasterreliefnet.common.event.payload.DisasterUpdatedPayload;
import com.disasterreliefnet.common.exception.BusinessRuleException;
import com.disasterreliefnet.common.exception.ResourceNotFoundException;
import com.disasterreliefnet.common.outbox.OutboxService;
import com.disasterreliefnet.disaster.domain.Disaster;
import com.disasterreliefnet.disaster.domain.DisasterUpdate;
import com.disasterreliefnet.disaster.dto.*;
import com.disasterreliefnet.disaster.mapper.DisasterMapper;
import com.disasterreliefnet.disaster.mapper.DisasterUpdateMapper;
import com.disasterreliefnet.disaster.repository.DisasterRepository;
import com.disasterreliefnet.disaster.repository.DisasterUpdateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service managing disaster lifecycle, situational updates, and event publishing.
 */
@Service
@Transactional
public class DisasterService {

    private static final Logger log = LoggerFactory.getLogger(DisasterService.class);

    private final DisasterRepository disasterRepository;
    private final DisasterUpdateRepository disasterUpdateRepository;
    private final DisasterMapper disasterMapper;
    private final DisasterUpdateMapper disasterUpdateMapper;
    private final OutboxService outboxService;

    public DisasterService(
        DisasterRepository disasterRepository,
        DisasterUpdateRepository disasterUpdateRepository,
        DisasterMapper disasterMapper,
        DisasterUpdateMapper disasterUpdateMapper,
        OutboxService outboxService
    ) {
        this.disasterRepository = disasterRepository;
        this.disasterUpdateRepository = disasterUpdateRepository;
        this.disasterMapper = disasterMapper;
        this.disasterUpdateMapper = disasterUpdateMapper;
        this.outboxService = outboxService;
    }

    /**
     * Declares a new disaster incident, stores it, and enqueues DisasterDeclared event.
     */
    public DisasterResponse createDisaster(CreateDisasterRequest request, String createdBy) {
        log.info("Declaring new disaster: name='{}', type={}, severity={}, createdBy='{}'",
            request.name(), request.type(), request.severityLevel(), createdBy);

        Disaster disaster = disasterMapper.toEntity(request);
        Disaster saved = disasterRepository.save(disaster);

        // Transactional outbox event
        AffectedAreaDto areaDto = new AffectedAreaDto(
            saved.getLatitude(),
            saved.getLongitude(),
            saved.getRadiusKm(),
            saved.getRegionName(),
            saved.getCountry()
        );

        DisasterDeclaredPayload payload = new DisasterDeclaredPayload(
            saved.getDisasterId(),
            saved.getName(),
            saved.getType(),
            saved.getSeverityLevel(),
            areaDto,
            saved.getOccurredAt()
        );

        outboxService.enqueue(
            KafkaTopics.DISASTER_DECLARED,
            "DISASTER",
            saved.getDisasterId(),
            "DisasterDeclared",
            payload,
            null
        );

        log.info("Disaster declared successfully: disasterId={}", saved.getDisasterId());
        return disasterMapper.toResponse(saved);
    }

    /**
     * Retrieves a disaster by ID.
     */
    @Transactional(readOnly = true)
    public DisasterResponse getDisasterById(UUID disasterId) {
        Disaster disaster = disasterRepository.findByDisasterIdAndDeletedAtIsNull(disasterId)
            .orElseThrow(() -> new ResourceNotFoundException("Disaster not found with id: " + disasterId));
        return disasterMapper.toResponse(disaster);
    }

    /**
     * Lists or filters disasters by status and/or disaster type.
     */
    @Transactional(readOnly = true)
    public Page<DisasterResponse> getDisasters(DisasterStatus status, DisasterType type, Pageable pageable) {
        Page<Disaster> page;
        if (status != null && type != null) {
            page = disasterRepository.findByStatusAndTypeAndDeletedAtIsNull(status, type, pageable);
        } else if (status != null) {
            page = disasterRepository.findByStatusAndDeletedAtIsNull(status, pageable);
        } else if (type != null) {
            page = disasterRepository.findByTypeAndDeletedAtIsNull(type, pageable);
        } else {
            page = disasterRepository.findByDeletedAtIsNull(pageable);
        }
        return page.map(disasterMapper::toResponse);
    }

    /**
     * Updates an active disaster's severity, description, affected area, or estimated casualties.
     */
    public DisasterResponse updateDisaster(UUID disasterId, UpdateDisasterRequest request, String updatedBy) {
        log.info("Updating disaster: disasterId={}, updatedBy='{}'", disasterId, updatedBy);

        Disaster disaster = disasterRepository.findByDisasterIdAndDeletedAtIsNull(disasterId)
            .orElseThrow(() -> new ResourceNotFoundException("Disaster not found with id: " + disasterId));

        if (disaster.getStatus() == DisasterStatus.CLOSED) {
            throw new BusinessRuleException("Cannot update a closed disaster incident: " + disasterId);
        }

        if (request.severityLevel() != null) {
            disaster.setSeverityLevel(request.severityLevel());
        }
        if (request.description() != null) {
            disaster.setDescription(request.description());
        }
        if (request.estimatedAffectedPeople() != null) {
            disaster.setEstimatedAffectedPeople(request.estimatedAffectedPeople());
        }
        if (request.affectedArea() != null) {
            GeoAreaDto area = request.affectedArea();
            if (area.latitude() != null) disaster.setLatitude(area.latitude());
            if (area.longitude() != null) disaster.setLongitude(area.longitude());
            if (area.radiusKm() != null) disaster.setRadiusKm(area.radiusKm());
            if (area.regionName() != null) disaster.setRegionName(area.regionName());
            if (area.country() != null) disaster.setCountry(area.country());
        }

        Disaster saved = disasterRepository.save(disaster);

        DisasterUpdatedPayload payload = new DisasterUpdatedPayload(
            saved.getDisasterId(),
            saved.getSeverityLevel(),
            (int) Math.min(saved.getEstimatedAffectedPeople(), Integer.MAX_VALUE),
            updatedBy != null ? updatedBy : "system"
        );

        outboxService.enqueue(
            KafkaTopics.DISASTER_UPDATED,
            "DISASTER",
            saved.getDisasterId(),
            "DisasterUpdated",
            payload,
            null
        );

        log.info("Disaster updated successfully: disasterId={}", saved.getDisasterId());
        return disasterMapper.toResponse(saved);
    }

    /**
     * Closes an active disaster incident with a closure rationale.
     */
    public DisasterResponse closeDisaster(UUID disasterId, CloseDisasterRequest request, String closedBy) {
        log.info("Closing disaster: disasterId={}, closedBy='{}', reason='{}'",
            disasterId, closedBy, request.closureReason());

        Disaster disaster = disasterRepository.findByDisasterIdAndDeletedAtIsNull(disasterId)
            .orElseThrow(() -> new ResourceNotFoundException("Disaster not found with id: " + disasterId));

        if (disaster.getStatus() == DisasterStatus.CLOSED) {
            throw new BusinessRuleException("Disaster is already closed: " + disasterId);
        }

        disaster.close(request.closureReason());
        Disaster saved = disasterRepository.save(disaster);

        DisasterClosedPayload payload = new DisasterClosedPayload(
            saved.getDisasterId(),
            saved.getClosureReason(),
            closedBy != null ? closedBy : "admin",
            saved.getClosedAt()
        );

        outboxService.enqueue(
            KafkaTopics.DISASTER_CLOSED,
            "DISASTER",
            saved.getDisasterId(),
            "DisasterClosed",
            payload,
            null
        );

        log.info("Disaster closed successfully: disasterId={}", saved.getDisasterId());
        return disasterMapper.toResponse(saved);
    }

    /**
     * Publishes a situational update or advisory for a disaster.
     */
    public DisasterUpdateResponse addDisasterUpdate(UUID disasterId, CreateDisasterUpdateRequest request, String postedBy) {
        log.info("Adding situational update to disaster: disasterId={}, title='{}', postedBy='{}'",
            disasterId, request.title(), postedBy);

        Disaster disaster = disasterRepository.findByDisasterIdAndDeletedAtIsNull(disasterId)
            .orElseThrow(() -> new ResourceNotFoundException("Disaster not found with id: " + disasterId));

        if (disaster.getStatus() == DisasterStatus.CLOSED) {
            throw new BusinessRuleException("Cannot add update to a closed disaster incident: " + disasterId);
        }

        DisasterUpdate update = disasterUpdateMapper.toEntity(request, disaster, postedBy != null ? postedBy : "anonymous");
        DisasterUpdate saved = disasterUpdateRepository.save(update);

        log.info("Disaster update saved: updateId={}, disasterId={}", saved.getUpdateId(), disasterId);
        return disasterUpdateMapper.toResponse(saved);
    }

    /**
     * Retrieves paged situational updates for a disaster.
     */
    @Transactional(readOnly = true)
    public Page<DisasterUpdateResponse> getDisasterUpdates(UUID disasterId, Pageable pageable) {
        if (!disasterRepository.existsById(disasterId)) {
            throw new ResourceNotFoundException("Disaster not found with id: " + disasterId);
        }
        return disasterUpdateRepository.findByDisaster_DisasterIdOrderByCreatedAtDesc(disasterId, pageable)
            .map(disasterUpdateMapper::toResponse);
    }
}
