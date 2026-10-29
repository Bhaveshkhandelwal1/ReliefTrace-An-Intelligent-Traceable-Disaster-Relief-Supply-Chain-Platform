package com.disasterreliefnet.verification.exception;

import com.disasterreliefnet.common.exception.BusinessRuleException;

public class InvalidCaseStatusException extends BusinessRuleException {
    public InvalidCaseStatusException(String message) {
        super(message);
    }
}
