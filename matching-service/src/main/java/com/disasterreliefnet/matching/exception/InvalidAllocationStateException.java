package com.disasterreliefnet.matching.exception;

import com.disasterreliefnet.common.exception.BusinessRuleException;

public class InvalidAllocationStateException extends BusinessRuleException {
    public InvalidAllocationStateException(String message) {
        super(message);
    }
}
