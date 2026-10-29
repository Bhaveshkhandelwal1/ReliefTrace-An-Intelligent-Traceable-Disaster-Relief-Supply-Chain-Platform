package com.disasterreliefnet.verification.exception;

import com.disasterreliefnet.common.exception.ConflictException;

public class DuplicateCaseException extends ConflictException {
    public DuplicateCaseException(String message) {
        super(message);
    }
}
