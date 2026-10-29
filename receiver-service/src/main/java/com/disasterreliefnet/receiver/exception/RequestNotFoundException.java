package com.disasterreliefnet.receiver.exception;

import com.disasterreliefnet.common.exception.ResourceNotFoundException;

import java.util.UUID;

public class RequestNotFoundException extends ResourceNotFoundException {
    public RequestNotFoundException(UUID requestId) {
        super("AidRequest", requestId);
    }
}
