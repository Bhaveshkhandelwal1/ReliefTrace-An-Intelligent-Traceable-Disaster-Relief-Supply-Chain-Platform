package com.disasterreliefnet.inventory.client;

import com.disasterreliefnet.inventory.client.dto.DisasterLocationDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * Feign client for invoking Disaster Service synchronously with Resilience4j fallback.
 */
@FeignClient(
    name = "disaster-service",
    fallback = DisasterServiceClientFallback.class
)
public interface DisasterServiceClient {

    @GetMapping("/api/v1/disasters/{disasterId}")
    DisasterLocationDto getDisasterById(@PathVariable("disasterId") UUID disasterId);
}
