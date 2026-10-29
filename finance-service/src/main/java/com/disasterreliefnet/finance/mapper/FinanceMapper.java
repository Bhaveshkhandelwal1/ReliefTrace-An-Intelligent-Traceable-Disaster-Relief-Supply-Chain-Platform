package com.disasterreliefnet.finance.mapper;

import com.disasterreliefnet.finance.domain.Disbursement;
import com.disasterreliefnet.finance.domain.LedgerEntry;
import com.disasterreliefnet.finance.domain.PaymentRecord;
import com.disasterreliefnet.finance.dto.DisbursementResponse;
import com.disasterreliefnet.finance.dto.LedgerEntryResponse;
import com.disasterreliefnet.finance.dto.PaymentRecordResponse;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper for Finance Service entities and DTOs.
 */
@Mapper(componentModel = "spring")
public interface FinanceMapper {

    PaymentRecordResponse toPaymentRecordResponse(PaymentRecord record);

    DisbursementResponse toDisbursementResponse(Disbursement disbursement);

    LedgerEntryResponse toLedgerEntryResponse(LedgerEntry ledgerEntry);
}
