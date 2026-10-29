package com.disasterreliefnet.transport.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * OpenFeign client for communicating synchronously with Disaster Service.
 */
@FeignClient(name = "disaster-service", fallback = DisasterClientFallback.class)
public interface DisasterClient {

    @GetMapping("/api/v1/disasters/{disasterId}")
    DisasterClientDto getDisasterById(@PathVariable("disasterId") UUID disasterId);
}
