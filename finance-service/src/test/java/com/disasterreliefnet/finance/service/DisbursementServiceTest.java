package com.disasterreliefnet.finance.service;

import com.disasterreliefnet.common.dto.DeliveryAddressDto;
import com.disasterreliefnet.common.enums.DisbursementStatus;
import com.disasterreliefnet.common.event.payload.AllocationApprovedPayload;
import com.disasterreliefnet.common.exception.BusinessRuleException;
import com.disasterreliefnet.common.exception.ResourceNotFoundException;
import com.disasterreliefnet.finance.domain.Disbursement;
import com.disasterreliefnet.finance.domain.LedgerEntryType;
import com.disasterreliefnet.finance.dto.AuthorizeDisbursementRequest;
import com.disasterreliefnet.finance.dto.DisbursementResponse;
import com.disasterreliefnet.finance.mapper.FinanceMapper;
import com.disasterreliefnet.finance.mapper.FinanceMapperImpl;
import com.disasterreliefnet.finance.repository.DisbursementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DisbursementServiceTest {

    @Mock
    private DisbursementRepository disbursementRepository;

    @Mock
    private LedgerService ledgerService;

    private final FinanceMapper financeMapper = new FinanceMapperImpl();
    private DisbursementService disbursementService;

    @BeforeEach
    void setUp() {
        disbursementService = new DisbursementService(
            disbursementRepository,
            ledgerService,
            financeMapper
        );
    }

    @Test
    @DisplayName("Should create pending disbursement from approved allocation")
    void createDisbursementFromAllocation_Success() {
        UUID allocationId = UUID.randomUUID();
        AllocationApprovedPayload payload = new AllocationApprovedPayload(
            allocationId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            BigDecimal.valueOf(25),
            "BOXES",
            "coordinator-1",
            new DeliveryAddressDto("123 Aid Way", "Dallas", "TX", "US", "75001")
        );

        when(disbursementRepository.existsByAllocationIdAndDeletedAtIsNull(allocationId)).thenReturn(false);
        when(disbursementRepository.save(any(Disbursement.class))).thenAnswer(i -> {
            Disbursement d = i.getArgument(0);
            d.setDisbursementId(UUID.randomUUID());
            return d;
        });

        disbursementService.createDisbursementFromAllocation(payload);

        verify(disbursementRepository).save(any(Disbursement.class));
    }

    @Test
    @DisplayName("Should authorize disbursement and write debit to ledger")
    void authorizeDisbursement_Success() {
        UUID disbursementId = UUID.randomUUID();
        UUID allocationId = UUID.randomUUID();
        Disbursement disbursement = new Disbursement();
        disbursement.setDisbursementId(disbursementId);
        disbursement.setAllocationId(allocationId);
        disbursement.setAmount(BigDecimal.valueOf(250.00));
        disbursement.setCurrency("USD");
        disbursement.setStatus(DisbursementStatus.PENDING);

        when(disbursementRepository.findByDisbursementIdAndDeletedAtIsNull(disbursementId)).thenReturn(Optional.of(disbursement));
        when(disbursementRepository.save(any(Disbursement.class))).thenAnswer(i -> i.getArgument(0));

        AuthorizeDisbursementRequest request = new AuthorizeDisbursementRequest("finance-admin", "Approved for transport fuel grant");
        DisbursementResponse response = disbursementService.authorizeDisbursement(disbursementId, request);

        assertNotNull(response);
        assertEquals(DisbursementStatus.PAID, response.status());
        assertEquals("finance-admin", response.authorizedBy());

        verify(ledgerService).recordEntry(
            eq(LedgerEntryType.DISBURSEMENT_OUT),
            eq(BigDecimal.valueOf(250.00)),
            eq("USD"),
            eq(allocationId),
            contains("Approved for transport fuel grant")
        );
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when authorizing already paid disbursement")
    void authorizeDisbursement_AlreadyPaid() {
        UUID disbursementId = UUID.randomUUID();
        Disbursement disbursement = new Disbursement();
        disbursement.setDisbursementId(disbursementId);
        disbursement.setStatus(DisbursementStatus.PAID);

        when(disbursementRepository.findByDisbursementIdAndDeletedAtIsNull(disbursementId)).thenReturn(Optional.of(disbursement));

        AuthorizeDisbursementRequest request = new AuthorizeDisbursementRequest("admin", "note");
        assertThrows(BusinessRuleException.class, () -> disbursementService.authorizeDisbursement(disbursementId, request));
    }

    @Test
    @DisplayName("Should retrieve disbursement by ID")
    void getDisbursementById_Success() {
        UUID disbursementId = UUID.randomUUID();
        Disbursement disbursement = new Disbursement();
        disbursement.setDisbursementId(disbursementId);
        disbursement.setAmount(BigDecimal.valueOf(100.00));

        when(disbursementRepository.findByDisbursementIdAndDeletedAtIsNull(disbursementId)).thenReturn(Optional.of(disbursement));

        DisbursementResponse response = disbursementService.getDisbursementById(disbursementId);

        assertNotNull(response);
        assertEquals(disbursementId, response.disbursementId());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when disbursement not found")
    void getDisbursementById_NotFound() {
        UUID disbursementId = UUID.randomUUID();
        when(disbursementRepository.findByDisbursementIdAndDeletedAtIsNull(disbursementId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> disbursementService.getDisbursementById(disbursementId));
    }
}
