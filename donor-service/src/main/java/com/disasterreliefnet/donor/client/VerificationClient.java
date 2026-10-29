package com.disasterreliefnet.donor.client;

import com.disasterreliefnet.donor.client.dto.VerificationStatusDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(
    name = "verification-service",
    fallback = VerificationClientFallback.class
)
public interface VerificationClient {

    @GetMapping("/api/v1/verification/donors/{donorId}/status")
    ResponseEntity<VerificationStatusDto> getDonorVerificationStatus(@PathVariable("donorId") UUID donorId);
}
