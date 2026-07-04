package com.shopflow.inventory_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "stock", schema = "inventory")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "product_id", nullable = false, unique = true)
    private UUID productId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Integer reserved;

    @Version
    private Long version;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public boolean hasEnoughStock(int requestedQuantity) {
        return (quantity - reserved) >= requestedQuantity;
    }

    public void reserve(int qty) {
        if (!hasEnoughStock(qty)) {
            throw new IllegalStateException(
                    "Insufficient stock for product: " + productId);
        }
        this.reserved += qty;
    }

    public void release(int qty) {
        this.reserved = Math.max(0, this.reserved - qty);
    }

    public void deduct(int qty) {
        this.quantity -= qty;
        this.reserved = Math.max(0, this.reserved - qty);
    }
}
