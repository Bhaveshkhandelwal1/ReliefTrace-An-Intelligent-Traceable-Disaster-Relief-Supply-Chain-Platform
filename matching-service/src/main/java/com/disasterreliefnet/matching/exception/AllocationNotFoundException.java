package com.disasterreliefnet.matching.exception;

import com.disasterreliefnet.common.exception.ResourceNotFoundException;

import java.util.UUID;

public class AllocationNotFoundException extends ResourceNotFoundException {
    public AllocationNotFoundException(UUID allocationId) {
        super("Allocation", allocationId);
    }

    public AllocationNotFoundException(String message) {
        super(message);
    }
}
