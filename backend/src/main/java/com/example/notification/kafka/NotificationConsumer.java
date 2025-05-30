package com.example.notification.kafka;

import com.example.notification.dto.NotificationEvent;
import com.example.notification.service.NotificationProcessingOrchestrator; // Updated
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NotificationConsumer {

    private final NotificationProcessingOrchestrator orchestrator; // Updated

    @Value("${notification.kafka.topics.notifications}")
    private String notificationsTopic;

    public NotificationConsumer(NotificationProcessingOrchestrator orchestrator) { // Updated
        this.orchestrator = orchestrator;
    }

    @KafkaListener(topics = "${notification.kafka.topics.notifications}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(NotificationEvent event) {
        log.info("Received notification event from topic {}: {}", notificationsTopic, event);
        try {
            orchestrator.processNotification(event, false); // Updated
        } catch (Exception e) {
            log.error("Error processing notification event: {}", e.getMessage(), e);
            // DLQ logic
        }
    }
}