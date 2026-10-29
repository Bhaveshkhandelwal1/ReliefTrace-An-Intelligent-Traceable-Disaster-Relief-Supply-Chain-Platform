package com.disasterreliefnet.inventory.client;

import com.disasterreliefnet.inventory.client.dto.DisasterLocationDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Fallback implementation invoked by Resilience4j when disaster-service is degraded or unavailable.
 */
@Component
public class DisasterServiceClientFallback implements DisasterServiceClient {

    private static final Logger log = LoggerFactory.getLogger(DisasterServiceClientFallback.class);

    @Override
    public DisasterLocationDto getDisasterById(UUID disasterId) {
        log.warn("Resilience4j fallback triggered for DisasterServiceClient.getDisasterById({})", disasterId);
        return null;
    }
}
