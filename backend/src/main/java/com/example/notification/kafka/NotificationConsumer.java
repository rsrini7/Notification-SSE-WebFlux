package com.example.notification.kafka;

import com.example.notification.dto.NotificationEvent;
import com.example.notification.dto.NotificationResponse;
import com.example.notification.model.Notification;
import com.example.notification.service.NotificationPersistenceService;
import com.example.notification.service.SseEmitterManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NotificationConsumer {

    private final NotificationPersistenceService persistenceService;
    private final SseEmitterManager sseEmitterManager;

    @Value("${notification.kafka.topics.notifications}")
    private String notificationsTopic;

    public NotificationConsumer(NotificationPersistenceService persistenceService, SseEmitterManager sseEmitterManager) {
        this.persistenceService = persistenceService;
        this.sseEmitterManager = sseEmitterManager;
    }

    @KafkaListener(topics = "${notification.kafka.topics.notifications}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(NotificationEvent event) {
        log.info("Received notification event from topic {}: {}", notificationsTopic, event);
        try {
            for (String userId : event.getTargetUserIds()) {
                Notification savedNotification = persistenceService.persistNotification(event, userId);
                NotificationResponse response = persistenceService.convertToResponse(savedNotification);
                sseEmitterManager.sendToUser(userId, response);
            }
        } catch (Exception e) {
            log.error("Error processing notification event: {}", e.getMessage(), e);
            // DLQ logic
        }
    }
}