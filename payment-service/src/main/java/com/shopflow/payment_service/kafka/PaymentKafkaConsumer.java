package com.shopflow.payment_service.kafka;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopflow.payment_service.event.OrderCreatedEvent;
import com.shopflow.payment_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentKafkaConsumer {

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order.created", groupId = "payment-service")
    public void handleOrderCreated(ConsumerRecord<String, String> record) {
        try {
            UUID eventId = UUID.fromString(record.key());
            OrderCreatedEvent event = objectMapper.readValue(
                    record.value(), OrderCreatedEvent.class);

            log.info("Received order.created event for order: {}", event.getOrderId());
            paymentService.processPayment(event, eventId);

        } catch (Exception e) {
            log.error("Failed to process order.created event: {}", e.getMessage(), e);
        }
    }
}
