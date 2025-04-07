package com.example.notification.kafka;

import com.example.notification.dto.NotificationEvent;
import com.example.notification.service.NotificationProcessorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for critical notifications that may require email delivery
 */
@Component
@Slf4j
public class CriticalNotificationConsumer {

    private final NotificationProcessorService processorService;

    @Value("${notification.kafka.topics.critical-notifications}")
    private String criticalNotificationsTopic;

    public CriticalNotificationConsumer(NotificationProcessorService processorService) {
        this.processorService = processorService;
    }

    @KafkaListener(topics = "${notification.kafka.topics.critical-notifications}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(NotificationEvent event) {
        log.info("Received critical notification event from topic {}: {}", criticalNotificationsTopic, event);
        try {
            // Process as critical notification (with email delivery)
            processorService.processNotification(event, true);
        } catch (Exception e) {
            log.error("Error processing critical notification event: {}", e.getMessage(), e);
            // In a production system, we would send to a Dead Letter Queue (DLQ)
            // kafkaTemplate.send("critical-notifications-dlq", event);
        }
    }
}