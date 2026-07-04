package com.shopflow.payment_service.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopflow.payment_service.dto.PaymentResponse;
import com.shopflow.payment_service.entity.OutboxEvent;
import com.shopflow.payment_service.entity.Payment;
import com.shopflow.payment_service.entity.ProcessedEvent;
import com.shopflow.payment_service.event.OrderCreatedEvent;
import com.shopflow.payment_service.event.PaymentCompletedEvent;
import com.shopflow.payment_service.event.PaymentFailedEvent;
import com.shopflow.payment_service.repository.OutboxRepository;
import com.shopflow.payment_service.repository.PaymentRepository;
import com.shopflow.payment_service.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OutboxRepository outboxRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void processPayment(OrderCreatedEvent event, UUID eventId) {
        // Идемпотентность
        if (processedEventRepository.existsByEventId(eventId)) {
            log.warn("Event already processed: {}", eventId);
            return;
        }

        // Создаём запись оплаты
        Payment payment = Payment.builder()
                .orderId(event.getOrderId())
                .userId(event.getUserId())
                .amount(event.getTotalAmount())
                .status(Payment.PaymentStatus.PENDING)
                .build();

        paymentRepository.save(payment);

        // Симулируем оплату — в реальном проекте здесь был бы вызов Stripe/Payme/Click
        boolean paymentSuccess = simulatePayment(event);

        if (paymentSuccess) {
            payment.setStatus(Payment.PaymentStatus.COMPLETED);

            PaymentCompletedEvent completedEvent = PaymentCompletedEvent.builder()
                    .paymentId(payment.getId())
                    .orderId(event.getOrderId())
                    .userId(event.getUserId())
                    .amount(event.getTotalAmount())
                    .build();

            saveOutboxEvent(event.getOrderId(), "payment.completed", completedEvent);
            log.info("Payment completed for order: {}", event.getOrderId());

        } else {
            payment.setStatus(Payment.PaymentStatus.FAILED);

            PaymentFailedEvent failedEvent = PaymentFailedEvent.builder()
                    .orderId(event.getOrderId())
                    .userId(event.getUserId())
                    .reason("Insufficient funds")
                    .build();

            saveOutboxEvent(event.getOrderId(), "payment.failed", failedEvent);
            log.warn("Payment failed for order: {}", event.getOrderId());
        }

        processedEventRepository.save(ProcessedEvent.builder()
                .eventId(eventId)
                .build());
    }

    @Transactional(readOnly = true)
    public PaymentResponse getByOrderId(UUID orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Payment not found for order: " + orderId));
        return PaymentResponse.from(payment);
    }

    private boolean simulatePayment(OrderCreatedEvent event) {
        // В реальном проекте — интеграция с платёжным шлюзом
        // Для учёбы просто возвращаем true
        return true;
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
