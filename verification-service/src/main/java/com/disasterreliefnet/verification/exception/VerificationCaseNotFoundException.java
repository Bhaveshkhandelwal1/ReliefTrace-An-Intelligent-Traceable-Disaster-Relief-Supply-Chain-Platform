package com.disasterreliefnet.verification.exception;

import com.disasterreliefnet.common.exception.ResourceNotFoundException;

import java.util.UUID;

public class VerificationCaseNotFoundException extends ResourceNotFoundException {
    public VerificationCaseNotFoundException(UUID caseId) {
        super("VerificationCase", caseId);
    }

    public VerificationCaseNotFoundException(String message) {
        super(message);
    }
}
