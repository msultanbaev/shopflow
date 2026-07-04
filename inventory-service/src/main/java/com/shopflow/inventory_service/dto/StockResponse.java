package com.shopflow.inventory_service.dto;


import com.shopflow.inventory_service.entity.Stock;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class StockResponse {

    private UUID productId;
    private Integer quantity;
    private Integer reserved;
    private Integer available;

    public static StockResponse from(Stock stock) {
        return StockResponse.builder()
                .productId(stock.getProductId())
                .quantity(stock.getQuantity())
                .reserved(stock.getReserved())
                .available(stock.getQuantity() - stock.getReserved())
                .build();
    }
}
