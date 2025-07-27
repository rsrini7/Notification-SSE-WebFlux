package com.example.notification.kafka;

import com.example.notification.dto.NotificationEvent;
import com.example.notification.dto.NotificationResponse;
import com.example.notification.model.Notification;
import com.example.notification.service.EmailService;
import com.example.notification.service.NotificationPersistenceService;
import com.example.notification.service.SseEmitterManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CriticalNotificationConsumer {

    private final NotificationPersistenceService persistenceService;
    private final SseEmitterManager sseEmitterManager;
    private final EmailService emailService;

    @Value("${notification.kafka.topics.critical-notifications}")
    private String criticalNotificationsTopic;

    public CriticalNotificationConsumer(NotificationPersistenceService persistenceService, SseEmitterManager sseEmitterManager, EmailService emailService) {
        this.persistenceService = persistenceService;
        this.sseEmitterManager = sseEmitterManager;
        this.emailService = emailService;
    }

    @KafkaListener(topics = "${notification.kafka.topics.critical-notifications}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(NotificationEvent event) {
        log.info("Received critical notification event from topic {}: {}", criticalNotificationsTopic, event);
        try {
            for (String userId : event.getTargetUserIds()) {
                Notification savedNotification = persistenceService.persistNotification(event, userId);
                NotificationResponse response = persistenceService.convertToResponse(savedNotification);
                sseEmitterManager.sendToUser(userId, response);
                emailService.sendNotificationEmail(userId, response);
            }
        } catch (Exception e) {
            log.error("Error processing critical notification event: {}", e.getMessage(), e);
            // DLQ logic
        }
    }
}