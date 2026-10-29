package com.disasterreliefnet.matching.client;

import com.disasterreliefnet.matching.client.dto.AidRequestClientDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ReceiverClientFallback implements ReceiverClient {

    private static final Logger log = LoggerFactory.getLogger(ReceiverClientFallback.class);

    @Override
    public AidRequestClientDto getAidRequest(UUID receiverId, UUID requestId) {
        log.warn("Fallback triggered: unable to fetch aid request {} from receiver-service", requestId);
        return null;
    }
}
