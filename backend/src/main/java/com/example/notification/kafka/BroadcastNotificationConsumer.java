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
        // Existing log:
        log.info("Received broadcast notification event from topic {}: Full Event: {}", broadcastNotificationsTopic, event); // Ensure full event is logged
        try {
            log.debug("Calling orchestrator.processBroadcastNotification for eventId: {}", event.getEventId());
            orchestrator.processBroadcastNotification(event);
            log.info("Successfully processed broadcast notification eventId: {} via orchestrator.", event.getEventId());
        } catch (Exception e) {
            log.error("Error after orchestrator.processBroadcastNotification for eventId: {}. Error: {}", event.getEventId(), e.getMessage(), e);
            // DLQ logic
        }
    }
}