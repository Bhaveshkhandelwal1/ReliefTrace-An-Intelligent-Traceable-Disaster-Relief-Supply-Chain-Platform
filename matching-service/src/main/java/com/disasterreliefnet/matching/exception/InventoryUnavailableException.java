package com.disasterreliefnet.matching.exception;

import com.disasterreliefnet.common.exception.BusinessRuleException;

public class InventoryUnavailableException extends BusinessRuleException {
    public InventoryUnavailableException(String message) {
        super(message);
    }
}
