package com.example.notification.kafka;

import com.example.notification.dto.NotificationEvent;
import com.example.notification.service.NotificationProcessorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for standard notifications
 */
@Component
@Slf4j
public class NotificationConsumer {

    private final NotificationProcessorService processorService;

    @Value("${notification.kafka.topics.notifications}")
    private String notificationsTopic;

    public NotificationConsumer(NotificationProcessorService processorService) {
        this.processorService = processorService;
    }

    @KafkaListener(topics = "${notification.kafka.topics.notifications}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(NotificationEvent event) {
        log.info("Received notification event from topic {}: {}", notificationsTopic, event);
        try {
            processorService.processNotification(event, false);
        } catch (Exception e) {
            log.error("Error processing notification event: {}", e.getMessage(), e);
            // In a production system, we would send to a Dead Letter Queue (DLQ)
            // kafkaTemplate.send("notifications-dlq", event);
        }
    }
}