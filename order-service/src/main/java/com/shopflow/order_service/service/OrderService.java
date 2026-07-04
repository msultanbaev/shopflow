package com.shopflow.order_service.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopflow.order_service.dto.*;
import com.shopflow.order_service.entity.Order;
import com.shopflow.order_service.entity.OrderItem;
import com.shopflow.order_service.entity.OutboxEvent;
import com.shopflow.order_service.event.OrderCancelledEvent;
import com.shopflow.order_service.event.OrderCreatedEvent;
import com.shopflow.order_service.repository.OrderRepository;
import com.shopflow.order_service.repository.OutboxRepository;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, UUID userId) {
        // 1. Считаем total (в реальном проекте цены берём из product-service)
        List<UUID> productIds = request.getItems().stream().map(OrderItemRequest::getProductId).toList();


        // 2. Создаём заказ
        Order order = Order.builder()
                .userId(userId)
                .status(Order.OrderStatus.PENDING)
                .build();

        List<OrderItem> items = request.getItems().stream()
                .map(item -> OrderItem.builder()
                        .order(order)
                        .productId(item.getProductId())
                        .quantity(item.getQuantity())
                        .build())
                .toList();

        order.setItems(items);
        orderRepository.save(order);

        // 3. Сохраняем событие в Outbox (в той же транзакции!)
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(order.getId())
                .userId(userId)
                .items(items.stream()
                        .map(item -> OrderCreatedEvent.OrderItem.builder()
                                .productId(item.getProductId())
                                .quantity(item.getQuantity())
                                .price(item.getPrice())
                                .build())
                        .toList())
                .build();

        saveOutboxEvent(order.getId(), "order.created", event);

        log.info("Order created: {}", order.getId());
        return OrderResponse.from(order);
    }

    @Transactional
    public void cancelOrder(UUID orderId, UUID userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (!order.getUserId().equals(userId)) {
            throw new IllegalArgumentException("You are not the owner of this order");
        }

        if (order.getStatus() == Order.OrderStatus.CONFIRMED) {
            throw new IllegalArgumentException("Cannot cancel confirmed order");
        }

        order.setStatus(Order.OrderStatus.CANCELLED);

        OrderCancelledEvent event = OrderCancelledEvent.builder()
                .orderId(orderId)
                .userId(userId)
                .reason("Cancelled by user")
                .build();

        saveOutboxEvent(orderId, "order.cancelled", event);
        log.info("Order cancelled: {}", orderId);
    }

    @Transactional
    public void confirmOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        order.setStatus(Order.OrderStatus.CONFIRMED);
        log.info("Order confirmed: {}", orderId);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getUserOrders(UUID userId, int page, int size) {
        return PageResponse.from(
                orderRepository.findByUserId(userId,
                                PageRequest.of(page, size, Sort.by("createdAt").descending()))
                        .map(OrderResponse::from)
        );
    }

    @Transactional(readOnly = true)
    public OrderResponse getById(UUID orderId, UUID userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (!order.getUserId().equals(userId)) {
            throw new IllegalArgumentException("You are not the owner of this order");
        }

        return OrderResponse.from(order);
    }

    @Transactional
    public void cancelOrderBySaga(UUID orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        order.setStatus(Order.OrderStatus.CANCELLED);

        List<OrderCancelledEvent.OrderItem> items = order.getItems().stream()
                .map(item -> OrderCancelledEvent.OrderItem.builder()
                        .productId(item.getProductId())
                        .quantity(item.getQuantity())
                        .build())
                .toList();

        OrderCancelledEvent event = OrderCancelledEvent.builder()
                .orderId(orderId)
                .userId(order.getUserId())
                .reason(reason)
                .items(items)
                .build();

        saveOutboxEvent(orderId, "order.cancelled", event);
        log.info("Order cancelled by Saga: {} reason: {}", orderId, reason);
    }

    private void saveOutboxEvent(UUID aggregateId, String eventType, Object payload) {
        try {
            outboxRepository.save(OutboxEvent.builder()
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .payload(objectMapper.writeValueAsString(payload))
                    .sent(false)
                    .build());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize event", e);
        }
    }
}
