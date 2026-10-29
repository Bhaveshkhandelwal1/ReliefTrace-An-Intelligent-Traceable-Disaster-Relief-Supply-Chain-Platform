package com.disasterreliefnet.receiver.exception;

import com.disasterreliefnet.common.exception.BusinessRuleException;

public class InvalidStatusTransitionException extends BusinessRuleException {
    public InvalidStatusTransitionException(String message) {
        super(message);
    }
}
