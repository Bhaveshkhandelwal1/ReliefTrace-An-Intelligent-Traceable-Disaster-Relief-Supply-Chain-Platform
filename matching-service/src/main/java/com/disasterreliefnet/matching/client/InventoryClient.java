package com.disasterreliefnet.matching.client;

import com.disasterreliefnet.common.enums.AidCategory;
import com.disasterreliefnet.matching.client.dto.ItemClientDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(
    name = "inventory-service",
    fallback = InventoryClientFallback.class
)
public interface InventoryClient {

    @GetMapping("/api/v1/inventory/items")
    Page<ItemClientDto> getItems(
        @RequestParam(value = "category", required = false) AidCategory category,
        @RequestParam(value = "warehouseId", required = false) UUID warehouseId,
        Pageable pageable
    );

    @GetMapping("/api/v1/inventory/items/{itemId}")
    ItemClientDto getItem(@PathVariable("itemId") UUID itemId);
}
