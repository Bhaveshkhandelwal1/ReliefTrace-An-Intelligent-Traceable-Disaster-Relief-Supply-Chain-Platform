package com.disasterreliefnet.inventory.controller;

import com.disasterreliefnet.common.enums.AidCategory;
import com.disasterreliefnet.inventory.dto.AdjustStockRequest;
import com.disasterreliefnet.inventory.dto.CreateItemRequest;
import com.disasterreliefnet.inventory.dto.ItemResponse;
import com.disasterreliefnet.inventory.dto.UpdateItemRequest;
import com.disasterreliefnet.inventory.service.ItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory/items")
@Tag(name = "Items", description = "Endpoints for catalog items, stock balances, and inventory adjustments")
public class ItemController {

    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    @Operation(summary = "Register inventory item", description = "Registers a new aid supply item in a specific warehouse")
    @ApiResponse(responseCode = "201", description = "Item registered successfully")
    public ResponseEntity<ItemResponse> createItem(@Valid @RequestBody CreateItemRequest request) {
        ItemResponse response = itemService.createItem(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR', 'DRIVER')")
    @Operation(summary = "List items", description = "Retrieves a paginated list of inventory items with real-time stock balances")
    public ResponseEntity<Page<ItemResponse>> getItems(
        @RequestParam(value = "category", required = false) AidCategory category,
        @RequestParam(value = "warehouseId", required = false) UUID warehouseId,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(itemService.getItems(category, warehouseId, pageable));
    }

    @GetMapping("/{itemId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR', 'DRIVER')")
    @Operation(summary = "Get item by ID", description = "Retrieves stock and batch details for a specific item")
    public ResponseEntity<ItemResponse> getItem(@PathVariable("itemId") UUID itemId) {
        return ResponseEntity.ok(itemService.getItem(itemId));
    }

    @PutMapping("/{itemId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    @Operation(summary = "Update item details", description = "Modifies name or expiry timestamp of an item")
    public ResponseEntity<ItemResponse> updateItem(
        @PathVariable("itemId") UUID itemId,
        @Valid @RequestBody UpdateItemRequest request
    ) {
        return ResponseEntity.ok(itemService.updateItem(itemId, request));
    }

    @PostMapping("/{itemId}/stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    @Operation(summary = "Adjust item stock", description = "Increases or decreases item stock level with an audit reason")
    public ResponseEntity<ItemResponse> adjustStock(
        @PathVariable("itemId") UUID itemId,
        @Valid @RequestBody AdjustStockRequest request,
        Authentication authentication
    ) {
        String adjustedBy = authentication != null ? authentication.getName() : "SYSTEM";
        return ResponseEntity.ok(itemService.adjustStock(itemId, request, adjustedBy));
    }
}
