package com.shopflow.inventory_service.service;


import com.shopflow.inventory_service.dto.StockRequest;
import com.shopflow.inventory_service.dto.StockResponse;
import com.shopflow.inventory_service.entity.ProcessedEvent;
import com.shopflow.inventory_service.entity.Stock;
import com.shopflow.inventory_service.event.OrderCancelledEvent;
import com.shopflow.inventory_service.event.OrderCreatedEvent;
import com.shopflow.inventory_service.repository.ProcessedEventRepository;
import com.shopflow.inventory_service.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final StockRepository stockRepository;
    private final ProcessedEventRepository processedEventRepository;

    @Transactional
    public StockResponse addStock(StockRequest request) {
        Stock stock = stockRepository.findByProductId(request.getProductId())
                .orElse(Stock.builder()
                        .productId(request.getProductId())
                        .quantity(0)
                        .reserved(0)
                        .build());

        stock.setQuantity(stock.getQuantity() + request.getQuantity());
        stockRepository.save(stock);

        log.info("Stock added: {} units for product: {}",
                request.getQuantity(), request.getProductId());
        return StockResponse.from(stock);
    }

    @Transactional
    public void reserveStock(OrderCreatedEvent event, UUID eventId) {
        // Идемпотентность — проверяем не обрабатывали ли уже это событие
        if (processedEventRepository.existsByEventId(eventId)) {
            log.warn("Event already processed: {}", eventId);
            return;
        }

        for (OrderCreatedEvent.OrderItem item : event.getItems()) {
            Stock stock = stockRepository
                    .findByProductIdWithLock(item.getProductId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Stock not found for product: " + item.getProductId()));

            stock.reserve(item.getQuantity());
            stockRepository.save(stock);

            log.info("Reserved {} units for product: {}",
                    item.getQuantity(), item.getProductId());
        }

        // Помечаем событие как обработанное
        processedEventRepository.save(ProcessedEvent.builder()
                .eventId(eventId)
                .build());
    }

    @Transactional
    public void releaseStock(UUID orderId, UUID eventId,
                             List<OrderCancelledEvent.OrderItem> items) {
        if (processedEventRepository.existsByEventId(eventId)) {
            log.warn("Event already processed: {}", eventId);
            return;
        }

        for (OrderCancelledEvent.OrderItem item : items) {
            stockRepository.findByProductIdWithLock(item.getProductId())
                    .ifPresent(stock -> {
                        stock.release(item.getQuantity());
                        stockRepository.save(stock);
                        log.info("Released {} units for product: {}",
                                item.getQuantity(), item.getProductId());
                    });
        }

        processedEventRepository.save(ProcessedEvent.builder()
                .eventId(eventId)
                .build());

        log.info("Stock released for order: {}", orderId);
    }

    @Transactional(readOnly = true)
    public StockResponse getStock(UUID productId) {
        Stock stock = stockRepository.findByProductId(productId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Stock not found for product: " + productId));
        return StockResponse.from(stock);
    }
}
