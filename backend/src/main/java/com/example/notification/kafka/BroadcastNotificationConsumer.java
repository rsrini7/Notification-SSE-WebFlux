package com.example.notification.kafka;

import com.example.notification.dto.NotificationEvent;
import com.example.notification.service.NotificationProcessingOrchestrator; // Updated
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class BroadcastNotificationConsumer {

    private final NotificationProcessingOrchestrator orchestrator; // Updated

    @Value("${notification.kafka.topics.broadcast-notifications}")
    private String broadcastNotificationsTopic;

    public BroadcastNotificationConsumer(NotificationProcessingOrchestrator orchestrator) { // Updated
        this.orchestrator = orchestrator;
    }

    @KafkaListener(topics = "${notification.kafka.topics.broadcast-notifications}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(NotificationEvent event) {
        log.info("Received broadcast notification event from topic {}: {}", broadcastNotificationsTopic, event);
        try {
            orchestrator.processBroadcastNotification(event); // Updated
        } catch (Exception e) {
            log.error("Error processing broadcast notification event: {}", e.getMessage(), e);
            // DLQ logic
        }
    }
}