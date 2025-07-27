package com.example.notification.kafka;

import com.example.notification.dto.NotificationEvent;
import com.example.notification.dto.NotificationResponse;
import com.example.notification.model.Notification;
import com.example.notification.model.NotificationPriority;
import com.example.notification.model.User;
import com.example.notification.repository.UserRepository;
import com.example.notification.service.EmailService;
import com.example.notification.service.NotificationPersistenceService;
import com.example.notification.service.SseEmitterManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class NotificationConsumer {

    private final NotificationPersistenceService persistenceService;
    private final SseEmitterManager sseEmitterManager;
    private final EmailService emailService;
    private final UserRepository userRepository;

    @Value("${notification.kafka.topics.notifications}")
    private String notificationsTopic;

    public NotificationConsumer(NotificationPersistenceService persistenceService, SseEmitterManager sseEmitterManager, EmailService emailService, UserRepository userRepository) {
        this.persistenceService = persistenceService;
        this.sseEmitterManager = sseEmitterManager;
        this.emailService = emailService;
        this.userRepository = userRepository;
    }

    @KafkaListener(topics = "${notification.kafka.topics.notifications}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(NotificationEvent event) {
        log.info("Received notification event from topic {}: {}", notificationsTopic, event);
        try {
            List<String> targetUserIds = event.getTargetUserIds();
            if (targetUserIds.contains("ALL")) {
                List<User> allUsers = userRepository.findAll();
                targetUserIds = allUsers.stream().map(User::getUsername).toList();
            }

            for (String userId : targetUserIds) {
                Notification savedNotification = persistenceService.persistNotification(event, userId);
                NotificationResponse response = persistenceService.convertToResponse(savedNotification);
                sseEmitterManager.sendToUser(userId, response);

                if (event.getPriority() == NotificationPriority.CRITICAL || event.isSendEmail()) {
                    emailService.sendNotificationEmail(userId, response);
                }
            }
        } catch (Exception e) {
            log.error("Error processing notification event: {}", e.getMessage(), e);
            // DLQ logic
        }
    }
}