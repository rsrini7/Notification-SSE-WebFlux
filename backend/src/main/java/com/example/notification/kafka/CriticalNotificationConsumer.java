package com.example.notification.kafka;

import com.example.notification.dto.NotificationEvent;
import com.example.notification.service.NotificationProcessingOrchestrator; // Updated
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CriticalNotificationConsumer {

    private final NotificationProcessingOrchestrator orchestrator; // Updated

    @Value("${notification.kafka.topics.critical-notifications}")
    private String criticalNotificationsTopic;

    public CriticalNotificationConsumer(NotificationProcessingOrchestrator orchestrator) { // Updated
        this.orchestrator = orchestrator;
    }

    @KafkaListener(topics = "${notification.kafka.topics.critical-notifications}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(NotificationEvent event) {
        log.info("Received critical notification event from topic {}: {}", criticalNotificationsTopic, event);
        try {
            orchestrator.processNotification(event, true); // Updated
        } catch (Exception e) {
            log.error("Error processing critical notification event: {}", e.getMessage(), e);
            // DLQ logic
        }
    }
}