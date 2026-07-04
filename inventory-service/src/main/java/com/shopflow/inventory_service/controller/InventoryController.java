package com.shopflow.inventory_service.controller;


import com.shopflow.inventory_service.dto.StockRequest;
import com.shopflow.inventory_service.dto.StockResponse;
import com.shopflow.inventory_service.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping("/stock")
    public ResponseEntity<StockResponse> addStock(
            @Valid @RequestBody StockRequest request) {
        return ResponseEntity.ok(inventoryService.addStock(request));
    }

    @GetMapping("/stock/{productId}")
    public ResponseEntity<StockResponse> getStock(
            @PathVariable UUID productId) {
        return ResponseEntity.ok(inventoryService.getStock(productId));
    }
}
