package com.disasterreliefnet.donor.client;

import com.disasterreliefnet.common.enums.VerificationStatus;
import com.disasterreliefnet.donor.client.dto.VerificationStatusDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class VerificationClientFallback implements VerificationClient {

    private static final Logger log = LoggerFactory.getLogger(VerificationClientFallback.class);

    @Override
    public ResponseEntity<VerificationStatusDto> getDonorVerificationStatus(UUID donorId) {
        log.warn("Resilience4j Fallback triggered for VerificationClient.getDonorVerificationStatus: donorId={}", donorId);
        return ResponseEntity.ok(new VerificationStatusDto(
            donorId,
            VerificationStatus.PENDING,
            "Verification service is temporarily unavailable. Status defaults to PENDING."
        ));
    }
}
