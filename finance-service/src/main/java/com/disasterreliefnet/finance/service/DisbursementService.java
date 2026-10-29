package com.disasterreliefnet.finance.service;

import com.disasterreliefnet.common.enums.DisbursementStatus;
import com.disasterreliefnet.common.event.payload.AllocationApprovedPayload;
import com.disasterreliefnet.common.exception.BusinessRuleException;
import com.disasterreliefnet.common.exception.ResourceNotFoundException;
import com.disasterreliefnet.finance.domain.Disbursement;
import com.disasterreliefnet.finance.domain.LedgerEntryType;
import com.disasterreliefnet.finance.dto.AuthorizeDisbursementRequest;
import com.disasterreliefnet.finance.dto.DisbursementResponse;
import com.disasterreliefnet.finance.mapper.FinanceMapper;
import com.disasterreliefnet.finance.repository.DisbursementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Service managing financial disbursements to drivers and emergency field operators.
 */
@Service
@Transactional(readOnly = true)
public class DisbursementService {

    private static final Logger log = LoggerFactory.getLogger(DisbursementService.class);

    private final DisbursementRepository disbursementRepository;
    private final LedgerService ledgerService;
    private final FinanceMapper financeMapper;

    public DisbursementService(
        DisbursementRepository disbursementRepository,
        LedgerService ledgerService,
        FinanceMapper financeMapper
    ) {
        this.disbursementRepository = disbursementRepository;
        this.ledgerService = ledgerService;
        this.financeMapper = financeMapper;
    }

    @Transactional
    public void createDisbursementFromAllocation(AllocationApprovedPayload payload) {
        log.info("Creating pending disbursement record for allocation: {}", payload.allocationId());

        if (disbursementRepository.existsByAllocationIdAndDeletedAtIsNull(payload.allocationId())) {
            log.info("Disbursement already exists for allocation {}, skipping duplicate", payload.allocationId());
            return;
        }

        // Standard operational stipend or allocation grant amount based on relief quantity
        BigDecimal amount = payload.quantity() != null && payload.quantity().compareTo(BigDecimal.ZERO) > 0
            ? payload.quantity().multiply(BigDecimal.valueOf(10)) // estimated stipend / goods cost
            : BigDecimal.valueOf(100.00);

        Disbursement disbursement = new Disbursement();
        disbursement.setAllocationId(payload.allocationId());
        disbursement.setAmount(amount);
        disbursement.setCurrency("USD");
        disbursement.setStatus(DisbursementStatus.PENDING);
        disbursement.setNotes("Emergency aid allocation grant for item " + payload.itemId());

        Disbursement saved = disbursementRepository.save(disbursement);
        log.info("Disbursement {} created for allocation {}", saved.getDisbursementId(), payload.allocationId());
    }

    @Transactional
    public DisbursementResponse authorizeDisbursement(UUID disbursementId, AuthorizeDisbursementRequest request) {
        log.info("Authorizing disbursement ID: {} by: {}", disbursementId, request.approvedBy());

        Disbursement disbursement = disbursementRepository.findByDisbursementIdAndDeletedAtIsNull(disbursementId)
            .orElseThrow(() -> new ResourceNotFoundException("Disbursement", disbursementId));

        if (disbursement.getStatus() != DisbursementStatus.PENDING && disbursement.getStatus() != DisbursementStatus.AUTHORIZED) {
            throw new BusinessRuleException("Cannot authorize disbursement in status: " + disbursement.getStatus());
        }

        Instant now = Instant.now();
        disbursement.setStatus(DisbursementStatus.PAID);
        disbursement.setAuthorizedBy(request.approvedBy());
        disbursement.setPaidAt(now);
        if (request.notes() != null && !request.notes().isBlank()) {
            disbursement.setNotes(request.notes());
        }

        Disbursement saved = disbursementRepository.save(disbursement);

        String desc = "Disbursement payout for allocation " + saved.getAllocationId() + " authorized by " + saved.getAuthorizedBy();
        if (saved.getNotes() != null && !saved.getNotes().isBlank()) {
            desc += " - " + saved.getNotes();
        }

        // Record entry in platform ledger
        ledgerService.recordEntry(
            LedgerEntryType.DISBURSEMENT_OUT,
            saved.getAmount(),
            saved.getCurrency(),
            saved.getAllocationId(),
            desc
        );

        log.info("Disbursement {} marked as PAID", saved.getDisbursementId());
        return financeMapper.toDisbursementResponse(saved);
    }

    public DisbursementResponse getDisbursementById(UUID disbursementId) {
        Disbursement disbursement = disbursementRepository.findByDisbursementIdAndDeletedAtIsNull(disbursementId)
            .orElseThrow(() -> new ResourceNotFoundException("Disbursement", disbursementId));
        return financeMapper.toDisbursementResponse(disbursement);
    }

    public Page<DisbursementResponse> findDisbursements(UUID allocationId, Pageable pageable) {
        if (allocationId != null) {
            return disbursementRepository.findAllByAllocationIdAndDeletedAtIsNull(allocationId, pageable)
                .map(financeMapper::toDisbursementResponse);
        }
        return disbursementRepository.findAllByDeletedAtIsNull(pageable)
            .map(financeMapper::toDisbursementResponse);
    }
}
