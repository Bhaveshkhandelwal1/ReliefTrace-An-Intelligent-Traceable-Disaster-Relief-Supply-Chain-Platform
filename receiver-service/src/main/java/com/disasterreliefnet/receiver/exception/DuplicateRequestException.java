package com.disasterreliefnet.receiver.exception;

import com.disasterreliefnet.common.exception.ConflictException;

public class DuplicateRequestException extends ConflictException {
    public DuplicateRequestException(String message) {
        super(message);
    }
}
