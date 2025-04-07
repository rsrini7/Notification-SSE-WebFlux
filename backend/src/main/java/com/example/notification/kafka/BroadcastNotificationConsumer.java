package com.example.notification.kafka;

import com.example.notification.dto.NotificationEvent;
import com.example.notification.service.NotificationProcessorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for broadcast notifications that are sent to all users
 */
@Component
@Slf4j
public class BroadcastNotificationConsumer {

    private final NotificationProcessorService processorService;

    @Value("${notification.kafka.topics.broadcast-notifications}")
    private String broadcastNotificationsTopic;

    public BroadcastNotificationConsumer(NotificationProcessorService processorService) {
        this.processorService = processorService;
    }

    @KafkaListener(topics = "${notification.kafka.topics.broadcast-notifications}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(NotificationEvent event) {
        log.info("Received broadcast notification event from topic {}: {}", broadcastNotificationsTopic, event);
        try {
            processorService.processBroadcastNotification(event);
        } catch (Exception e) {
            log.error("Error processing broadcast notification event: {}", e.getMessage(), e);
            // In a production system, we would send to a Dead Letter Queue (DLQ)
            // kafkaTemplate.send("broadcast-notifications-dlq", event);
        }
    }
}