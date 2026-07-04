package com.shopflow.product_service.service;

import com.shopflow.product_service.dto.*;
import com.shopflow.product_service.entity.Category;
import com.shopflow.product_service.entity.Product;
import com.shopflow.product_service.exception.ProductNotFoundException;
import com.shopflow.product_service.repository.CategoryRepository;
import com.shopflow.product_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    // ===== PRODUCTS =====

    @Transactional
    public ProductResponse create(ProductRequest request, UUID sellerId) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .category(category)
                .sellerId(sellerId)
                .imageUrl(request.getImageUrl())
                .stock(request.getStock())
                .status(Product.Status.ACTIVE)
                .build();

        Product savedProduct = productRepository.save(product);
        log.info("Product created: {}", product.getId());
        return ProductResponse.from(savedProduct);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return PageResponse.from(
                productRepository.findByStatus(Product.Status.ACTIVE, pageable)
                        .map(ProductResponse::from)
        );
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(UUID id) {
        return productRepository.findById(id)
                .map(ProductResponse::from)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getByCategory(UUID categoryId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return PageResponse.from(
                productRepository.findByCategoryIdAndStatus(categoryId, Product.Status.ACTIVE, pageable)
                        .map(ProductResponse::from)
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> search(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return PageResponse.from(
                productRepository.search(query, pageable)
                        .map(ProductResponse::from)
        );
    }

    @Transactional
    public ProductResponse update(UUID id, ProductRequest request, UUID sellerId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        if (!product.getSellerId().equals(sellerId)) {
            throw new IllegalArgumentException("You are not the owner of this product");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setCategory(category);
        product.setImageUrl(request.getImageUrl());
        product.setStock(request.getStock());

        log.info("Product updated: {}", product.getId());
        return ProductResponse.from(product);
    }

    @Transactional
    public void delete(UUID id, UUID sellerId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        if (!product.getSellerId().equals(sellerId)) {
            throw new IllegalArgumentException("You are not the owner of this product");
        }

        product.setStatus(Product.Status.INACTIVE);
        log.info("Product deleted: {}", id);
    }

    // ===== CATEGORIES =====

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Category already exists");
        }

        Category category = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();

        categoryRepository.save(category);
        return CategoryResponse.from(category);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll()
                .stream()
                .map(CategoryResponse::from)
                .toList();
    }

    private final StorageService storageService;

    @Transactional
    public ProductResponse uploadImage(UUID id, MultipartFile file, UUID sellerId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        if (!product.getSellerId().equals(sellerId)) {
            throw new IllegalArgumentException("You are not the owner of this product");
        }

        if (product.getImageUrl() != null) {
            storageService.deleteFile(product.getImageUrl());
        }

        String imageUrl = storageService.uploadFile(file);
        product.setImageUrl(imageUrl);

        log.info("Image uploaded for product: {}", id);
        return ProductResponse.from(product);
    }
}
