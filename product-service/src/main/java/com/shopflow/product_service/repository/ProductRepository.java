package com.shopflow.product_service.repository;

import com.shopflow.product_service.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    Page<Product> findByStatus(Product.Status status, Pageable pageable);

    Page<Product> findByCategoryIdAndStatus(UUID categoryId, Product.Status status, Pageable pageable);

    @Query("""
        SELECT p FROM Product p
        WHERE p.status = 'ACTIVE'
        AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%'))
        OR LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')))
        """)
    Page<Product> search(@Param("query") String query, Pageable pageable);

    Page<Product> findBySellerId(UUID sellerId, Pageable pageable);
}
