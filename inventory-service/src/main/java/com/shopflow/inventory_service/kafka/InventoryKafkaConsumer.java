package com.shopflow.inventory_service.kafka;



import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopflow.inventory_service.event.OrderCreatedEvent;
import com.shopflow.inventory_service.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryKafkaConsumer {

    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order.created", groupId = "inventory-service")
    public void handleOrderCreated(ConsumerRecord<String, String> record) {
        try {
            UUID eventId = UUID.fromString(record.key());
            OrderCreatedEvent event = objectMapper.readValue(
                    record.value(), OrderCreatedEvent.class);

            log.info("Received order.created event for order: {}", event.getOrderId());
            inventoryService.reserveStock(event, eventId);

        } catch (Exception e) {
            log.error("Failed to process order.created event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "order.cancelled", groupId = "inventory-service")
    public void handleOrderCancelled(ConsumerRecord<String, String> record) {
        try {
            UUID eventId = UUID.fromString(record.key());
            UUID orderId = UUID.fromString(record.value());

            log.info("Received order.cancelled event for order: {}", orderId);
            inventoryService.releaseStock(orderId, eventId);

        } catch (Exception e) {
            log.error("Failed to process order.cancelled event: {}", e.getMessage(), e);
        }
    }
}
