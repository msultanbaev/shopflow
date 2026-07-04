package com.shopflow.order_service.dto;

import com.shopflow.order_service.entity.OrderItem;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class OrderItemResponse {

    private UUID productId;
    private Integer quantity;
    private BigDecimal price;

    public static OrderItemResponse from(OrderItem item) {
        return OrderItemResponse.builder()
                .productId(item.getProductId())
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .build();
    }
}
