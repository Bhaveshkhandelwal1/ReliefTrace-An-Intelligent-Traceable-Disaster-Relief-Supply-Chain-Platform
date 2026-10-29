package com.disasterreliefnet.transport.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Resilience4j fallback implementation for DisasterClient.
 */
@Component
public class DisasterClientFallback implements DisasterClient {

    private static final Logger log = LoggerFactory.getLogger(DisasterClientFallback.class);

    @Override
    public DisasterClientDto getDisasterById(UUID disasterId) {
        log.warn("Disaster service unavailable. Falling back for disaster ID: {}", disasterId);
        return null;
    }
}
