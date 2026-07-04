package com.shopflow.product_service.dto;

import com.shopflow.product_service.entity.Product;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ProductResponse {

    private UUID id;
    private String name;
    private String description;
    private BigDecimal price;
    private CategoryResponse category;
    private UUID sellerId;
    private String imageUrl;
    private String status;
    private LocalDateTime createdAt;

    public static ProductResponse from(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .category(CategoryResponse.from(product.getCategory()))
                .sellerId(product.getSellerId())
                .imageUrl(product.getImageUrl()).status(product.getStatus().name())
                .createdAt(product.getCreatedAt())
                .build();
    }
}
