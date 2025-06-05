package com.example.notification.service;

import com.example.notification.dto.NotificationResponse;
import com.example.notification.model.Notification;
import com.example.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationDispatchService {

    private final SseEmitterManager sseEmitterManager;
    private final EmailService emailService;
    private final NotificationRepository notificationRepository; // Added

    public void dispatchNotification(String userId, NotificationResponse response) {
        // SseEmitterManager handles whether user is connected or has an emitter
        sseEmitterManager.sendToUser(userId, response);
        log.debug("Attempted to send notification ID {} to user {} via SSE", response.getId(), userId);
        // No need to check if user is connected here, SseEmitterManager handles it.
    }

    @Transactional
    public void dispatchToEmail(String userId, NotificationResponse response) {
        if (response == null || response.getId() == null) {
            log.warn("Cannot dispatch email for null response or response with no ID for user {}.", userId);
            return;
        }

        Notification notification = notificationRepository.findById(response.getId()).orElse(null);

        if (notification == null) {
            log.warn("Notification with ID {} not found. Cannot dispatch email to user {}.", response.getId(), userId);
            return;
        }

        if (notification.getEmailDispatchedAt() == null) {
            try {
                emailService.sendNotificationEmail(userId, response);
                notification.setEmailDispatchedAt(LocalDateTime.now());
                notificationRepository.save(notification);
                log.info("Successfully dispatched email for notification ID {} to user {}.", response.getId(), userId);
            } catch (Exception e) {
                log.error("Error during email dispatch or updating notification ID {}: {}", response.getId(), e.getMessage(), e);
                // Depending on requirements, we might not set emailDispatchedAt if email sending fails,
                // to allow for retries. For now, we assume if sendNotificationEmail throws, we don't mark as dispatched.
            }
        } else {
            log.info("Email for notification ID {} to user {} already dispatched at {}. Skipping.",
                    response.getId(), userId, notification.getEmailDispatchedAt());
        }
    }
}