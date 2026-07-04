package com.shopflow.payment_service.service;


import com.shopflow.payment_service.entity.OutboxEvent;
import com.shopflow.payment_service.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> events = outboxRepository.findBySentFalse();

        for (OutboxEvent event : events) {
            try {
                kafkaTemplate.send(event.getEventType(),
                        event.getAggregateId().toString(),
                        event.getPayload());
                event.setSent(true);
                log.info("Published event: {} for aggregate: {}",
                        event.getEventType(), event.getAggregateId());
            } catch (Exception e) {
                log.error("Failed to publish event: {}", event.getId(), e);
            }
        }

        outboxRepository.saveAll(events);
    }
}
