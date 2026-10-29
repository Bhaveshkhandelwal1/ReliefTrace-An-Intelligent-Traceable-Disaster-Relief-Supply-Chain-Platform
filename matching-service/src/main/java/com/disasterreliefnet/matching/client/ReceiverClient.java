package com.disasterreliefnet.matching.client;

import com.disasterreliefnet.matching.client.dto.AidRequestClientDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(
    name = "receiver-service",
    fallback = ReceiverClientFallback.class
)
public interface ReceiverClient {

    @GetMapping("/api/v1/receivers/{receiverId}/requests/{requestId}")
    AidRequestClientDto getAidRequest(
        @PathVariable("receiverId") UUID receiverId,
        @PathVariable("requestId") UUID requestId
    );
}
