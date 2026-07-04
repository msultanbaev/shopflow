package com.shopflow.order_service.kafka;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopflow.order_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderKafkaConsumer {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "payment.completed", groupId = "order-service")
    public void handlePaymentCompleted(ConsumerRecord<String, String> record) {
        try {
            UUID orderId = UUID.fromString(record.key());
            log.info("Payment completed for order: {}", orderId);
            orderService.confirmOrder(orderId);
        } catch (Exception e) {
            log.error("Failed to process payment.completed: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "payment.failed", groupId = "order-service")
    public void handlePaymentFailed(ConsumerRecord<String, String> record) {
        try {
            UUID orderId = UUID.fromString(record.key());
            log.info("Payment failed for order: {}", orderId);
            orderService.cancelOrderBySaga(orderId, "Payment failed");
        } catch (Exception e) {
            log.error("Failed to process payment.failed: {}", e.getMessage(), e);
        }
    }
}
