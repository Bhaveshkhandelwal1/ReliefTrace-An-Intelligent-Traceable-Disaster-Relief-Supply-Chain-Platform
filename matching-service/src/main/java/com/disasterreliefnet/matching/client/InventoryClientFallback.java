package com.disasterreliefnet.matching.client;

import com.disasterreliefnet.common.enums.AidCategory;
import com.disasterreliefnet.matching.client.dto.ItemClientDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;

@Component
public class InventoryClientFallback implements InventoryClient {

    private static final Logger log = LoggerFactory.getLogger(InventoryClientFallback.class);

    @Override
    public Page<ItemClientDto> getItems(AidCategory category, UUID warehouseId, Pageable pageable) {
        log.warn("Fallback triggered: unable to fetch inventory items from inventory-service");
        return new PageImpl<>(Collections.emptyList(), pageable, 0);
    }

    @Override
    public ItemClientDto getItem(UUID itemId) {
        log.warn("Fallback triggered: unable to fetch item {} from inventory-service", itemId);
        return null;
    }
}
