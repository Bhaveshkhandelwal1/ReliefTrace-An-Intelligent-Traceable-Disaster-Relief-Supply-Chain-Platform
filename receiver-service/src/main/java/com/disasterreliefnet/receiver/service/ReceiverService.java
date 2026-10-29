package com.disasterreliefnet.receiver.service;

import com.disasterreliefnet.common.dto.DeliveryAddressDto;
import com.disasterreliefnet.common.enums.RequestStatus;
import com.disasterreliefnet.common.enums.VerificationStatus;
import com.disasterreliefnet.common.event.KafkaTopics;
import com.disasterreliefnet.common.event.payload.RequestCreatedPayload;
import com.disasterreliefnet.common.outbox.OutboxService;
import com.disasterreliefnet.receiver.domain.AidRequest;
import com.disasterreliefnet.receiver.domain.Receiver;
import com.disasterreliefnet.receiver.dto.*;
import com.disasterreliefnet.receiver.exception.DuplicateRequestException;
import com.disasterreliefnet.receiver.exception.InvalidStatusTransitionException;
import com.disasterreliefnet.receiver.exception.ReceiverNotFoundException;
import com.disasterreliefnet.receiver.exception.RequestNotFoundException;
import com.disasterreliefnet.receiver.mapper.ReceiverMapper;
import com.disasterreliefnet.receiver.repository.AidRequestRepository;
import com.disasterreliefnet.receiver.repository.ReceiverRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Core business logic for receiver profiles and aid requests.
 */
@Service
public class ReceiverService {

    private static final Logger log = LoggerFactory.getLogger(ReceiverService.class);

    private final ReceiverRepository receiverRepository;
    private final AidRequestRepository aidRequestRepository;
    private final ReceiverMapper receiverMapper;
    private final OutboxService outboxService;

    public ReceiverService(
        ReceiverRepository receiverRepository,
        AidRequestRepository aidRequestRepository,
        ReceiverMapper receiverMapper,
        OutboxService outboxService
    ) {
        this.receiverRepository = receiverRepository;
        this.aidRequestRepository = aidRequestRepository;
        this.receiverMapper = receiverMapper;
        this.outboxService = outboxService;
    }

    // -------------------------------------------------------------------------
    // Receiver CRUD
    // -------------------------------------------------------------------------

    /**
     * Registers a new receiver profile.
     */
    @Transactional
    public ReceiverResponse registerReceiver(CreateReceiverRequest request) {
        log.info("Registering new receiver: email={}, disasterId={}", request.email(), request.disasterId());

        DeliveryAddressDto addr = request.currentLocation();
        Receiver receiver = Receiver.builder()
            .receiverId(UUID.randomUUID())
            .firstName(request.firstName())
            .lastName(request.lastName())
            .email(request.email())
            .phone(request.phone())
            .disasterId(request.disasterId())
            .street(addr.street())
            .city(addr.city())
            .state(addr.state())
            .country(addr.country())
            .zip(addr.zip())
            .householdSize(request.householdSize())
            .vulnerabilityTags(joinTags(request.vulnerabilityTags()))
            .verificationStatus(VerificationStatus.UNVERIFIED)
            .build();

        Receiver saved = receiverRepository.save(receiver);
        log.info("Receiver registered: receiverId={}", saved.getReceiverId());
        return receiverMapper.toReceiverResponse(saved);
    }

    /**
     * Retrieves a single non-deleted receiver by ID.
     */
    @Transactional(readOnly = true)
    public ReceiverResponse getReceiver(UUID receiverId) {
        return receiverMapper.toReceiverResponse(findActiveReceiver(receiverId));
    }

    /**
     * Updates mutable fields on a receiver profile.
     */
    @Transactional
    public ReceiverResponse updateReceiver(UUID receiverId, UpdateReceiverRequest request) {
        log.info("Updating receiver profile: receiverId={}", receiverId);
        Receiver receiver = findActiveReceiver(receiverId);

        if (request.phone() != null) {
            receiver.setPhone(request.phone());
        }
        if (request.currentLocation() != null) {
            DeliveryAddressDto addr = request.currentLocation();
            receiver.setStreet(addr.street());
            receiver.setCity(addr.city());
            receiver.setState(addr.state());
            receiver.setCountry(addr.country());
            receiver.setZip(addr.zip());
        }
        if (request.householdSize() > 0) {
            receiver.setHouseholdSize(request.householdSize());
        }
        if (request.vulnerabilityTags() != null) {
            receiver.setVulnerabilityTags(joinTags(request.vulnerabilityTags()));
        }

        return receiverMapper.toReceiverResponse(receiverRepository.save(receiver));
    }

    /**
     * Soft-deletes a receiver profile (sets deletedAt).
     */
    @Transactional
    public void deleteReceiver(UUID receiverId) {
        log.info("Soft-deleting receiver: receiverId={}", receiverId);
        Receiver receiver = findActiveReceiver(receiverId);
        receiver.setDeletedAt(Instant.now());
        receiverRepository.save(receiver);
    }

    /**
     * Lists all active receivers, optionally filtered by disasterId.
     */
    @Transactional(readOnly = true)
    public Page<ReceiverResponse> listReceivers(UUID disasterId, Pageable pageable) {
        Page<Receiver> page = (disasterId != null)
            ? receiverRepository.findAllByDisasterId(disasterId, pageable)
            : receiverRepository.findAllActive(pageable);
        return page.map(receiverMapper::toReceiverResponse);
    }

    // -------------------------------------------------------------------------
    // Aid Request operations
    // -------------------------------------------------------------------------

    /**
     * Submits a new aid request for the given receiver, enforcing duplicate detection.
     */
    @Transactional
    public AidRequestResponse createAidRequest(UUID receiverId, CreateAidRequestRequest request) {
        log.info("Creating aid request: receiverId={}, category={}", receiverId, request.category());
        Receiver receiver = findActiveReceiver(receiverId);

        // Duplicate detection: same receiver + disaster + category + unit
        String dedupKey = buildDedupKey(receiverId, request.disasterId(), request.category().name(), request.unit());
        if (aidRequestRepository.existsByDedupKeyAndDeletedAtIsNull(dedupKey)) {
            throw new DuplicateRequestException(
                "A pending aid request for the same category and unit already exists for this receiver in this disaster."
            );
        }

        DeliveryAddressDto addr = request.deliveryAddress();
        AidRequest aidRequest = AidRequest.builder()
            .requestId(UUID.randomUUID())
            .receiver(receiver)
            .disasterId(request.disasterId())
            .category(request.category())
            .description(request.description())
            .quantityRequested(request.quantityRequested())
            .unit(request.unit())
            .urgencyLevel(request.urgencyLevel())
            .deliveryStreet(addr.street())
            .deliveryCity(addr.city())
            .deliveryState(addr.state())
            .deliveryCountry(addr.country())
            .deliveryZip(addr.zip())
            .status(RequestStatus.PENDING)
            .dedupKey(dedupKey)
            .build();

        AidRequest saved = aidRequestRepository.save(aidRequest);

        // Publish RequestCreated via outbox (within same transaction)
        RequestCreatedPayload payload = new RequestCreatedPayload(
            saved.getRequestId(),
            receiverId,
            saved.getDisasterId(),
            saved.getCategory(),
            saved.getDescription(),
            saved.getQuantityRequested(),
            saved.getUnit(),
            saved.getUrgencyLevel(),
            addr,
            receiver.getEmail(),
            receiver.getFirstName() + " " + receiver.getLastName()
        );
        outboxService.enqueue(
            KafkaTopics.REQUEST_CREATED,
            "AID_REQUEST",
            saved.getRequestId(),
            "RequestCreated",
            payload,
            saved.getRequestId().toString()
        );

        log.info("Aid request created and outbox event enqueued: requestId={}", saved.getRequestId());
        return receiverMapper.toAidRequestResponse(saved);
    }

    /**
     * Lists aid requests for a specific receiver, optionally filtered by status.
     */
    @Transactional(readOnly = true)
    public Page<AidRequestResponse> listAidRequests(UUID receiverId, RequestStatus status, Pageable pageable) {
        findActiveReceiver(receiverId); // existence check
        Page<AidRequest> page = (status != null)
            ? aidRequestRepository.findAllByReceiverIdAndStatus(receiverId, status, pageable)
            : aidRequestRepository.findAllByReceiverId(receiverId, pageable);
        return page.map(receiverMapper::toAidRequestResponse);
    }

    /**
     * Retrieves a single aid request by ID, scoped to the given receiver.
     */
    @Transactional(readOnly = true)
    public AidRequestResponse getAidRequest(UUID receiverId, UUID requestId) {
        AidRequest aidRequest = aidRequestRepository.findByRequestIdAndReceiverId(requestId, receiverId)
            .orElseThrow(() -> new RequestNotFoundException(requestId));
        return receiverMapper.toAidRequestResponse(aidRequest);
    }

    /**
     * Cancels an aid request that has not yet been allocated.
     */
    @Transactional
    public AidRequestResponse cancelAidRequest(UUID receiverId, UUID requestId) {
        log.info("Cancelling aid request: requestId={}, receiverId={}", requestId, receiverId);
        AidRequest aidRequest = aidRequestRepository.findByRequestIdAndReceiverId(requestId, receiverId)
            .orElseThrow(() -> new RequestNotFoundException(requestId));

        if (aidRequest.getStatus() == RequestStatus.ALLOCATED
            || aidRequest.getStatus() == RequestStatus.IN_TRANSIT
            || aidRequest.getStatus() == RequestStatus.DELIVERED
            || aidRequest.getStatus() == RequestStatus.CANCELLED) {
            throw new InvalidStatusTransitionException(
                "Cannot cancel request in status: " + aidRequest.getStatus()
            );
        }

        aidRequest.setStatus(RequestStatus.CANCELLED);
        aidRequest.setDeletedAt(Instant.now());
        return receiverMapper.toAidRequestResponse(aidRequestRepository.save(aidRequest));
    }

    /**
     * Cross-cutting listing of all requests (admin/coordinator), optionally by disasterId + status.
     */
    @Transactional(readOnly = true)
    public Page<AidRequestResponse> listAllRequests(UUID disasterId, RequestStatus status, Pageable pageable) {
        Page<AidRequest> page;
        if (disasterId != null && status != null) {
            page = aidRequestRepository.findAllByDisasterIdAndStatus(disasterId, status, pageable);
        } else if (disasterId != null) {
            page = aidRequestRepository.findAllByDisasterId(disasterId, pageable);
        } else {
            page = aidRequestRepository.findAllActive(pageable);
        }
        return page.map(receiverMapper::toAidRequestResponse);
    }

    // -------------------------------------------------------------------------
    // Event-driven status transitions (called from Kafka consumers)
    // -------------------------------------------------------------------------

    /**
     * Transitions an aid request to VERIFIED or REJECTED based on the verification outcome.
     */
    @Transactional
    public void applyVerificationOutcome(UUID requestId, String outcome, String notes) {
        log.info("Applying verification outcome: requestId={}, outcome={}", requestId, outcome);
        AidRequest aidRequest = findActiveAidRequest(requestId);

        if ("APPROVED".equalsIgnoreCase(outcome)) {
            aidRequest.setStatus(RequestStatus.VERIFIED);
        } else {
            aidRequest.setStatus(RequestStatus.REJECTED);
        }
        aidRequestRepository.save(aidRequest);
    }

    /**
     * Records allocationId and transitions request to ALLOCATED state.
     */
    @Transactional
    public void applyAllocationApproved(UUID requestId, UUID allocationId) {
        log.info("Applying allocation approved: requestId={}, allocationId={}", requestId, allocationId);
        AidRequest aidRequest = findActiveAidRequest(requestId);
        aidRequest.setAllocationId(allocationId);
        aidRequest.setStatus(RequestStatus.ALLOCATED);
        aidRequestRepository.save(aidRequest);
    }

    /**
     * Marks an aid request as DELIVERED when the associated shipment is delivered.
     */
    @Transactional
    public void applyShipmentDelivered(UUID allocationId) {
        log.info("Marking aid request delivered for allocationId={}", allocationId);
        aidRequestRepository.findAllActive(Pageable.unpaged()).forEach(req -> {
            if (allocationId.equals(req.getAllocationId())) {
                req.setStatus(RequestStatus.DELIVERED);
                aidRequestRepository.save(req);
                log.info("Aid request marked DELIVERED: requestId={}", req.getRequestId());
            }
        });
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Receiver findActiveReceiver(UUID receiverId) {
        return receiverRepository.findActiveById(receiverId)
            .orElseThrow(() -> new ReceiverNotFoundException(receiverId));
    }

    private AidRequest findActiveAidRequest(UUID requestId) {
        return aidRequestRepository.findById(requestId)
            .filter(r -> r.getDeletedAt() == null)
            .orElseThrow(() -> new RequestNotFoundException(requestId));
    }

    private static String buildDedupKey(UUID receiverId, UUID disasterId, String category, String unit) {
        return receiverId + ":" + disasterId + ":" + category + ":" + unit.toLowerCase();
    }

    private static String joinTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return null;
        }
        return tags.stream().map(String::trim).collect(Collectors.joining(","));
    }
}
