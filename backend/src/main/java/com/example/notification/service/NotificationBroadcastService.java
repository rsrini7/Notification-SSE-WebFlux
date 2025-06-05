package com.example.notification.service;

import com.example.notification.dto.NotificationEvent;
import com.example.notification.dto.NotificationResponse;
import com.example.notification.model.Notification;
import com.example.notification.model.User;
import com.example.notification.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationBroadcastService {

    private final UserRepository userRepository;
    private final NotificationPersistenceService persistenceService;
    private final NotificationDispatchService dispatchService;

    @Transactional // This method involves multiple database operations and dispatches
    public void processBroadcast(NotificationEvent event) {
        log.info("Processing broadcast notification event: {}", event);

        List<User> users = userRepository.findAll();
        if (users.isEmpty()) {
            log.info("No users found to broadcast notification to.");
            return;
        }
        log.info("Found {} users for broadcast.", users.size());

        List<Notification> savedNotifications = persistenceService.persistBroadcastNotifications(event, users);
        log.info("{} broadcast notifications successfully saved to the database.", savedNotifications.size());

        int sseMessagesSent = 0;
        for (Notification savedNotification : savedNotifications) {
            try {
                NotificationResponse response = persistenceService.convertToResponse(savedNotification);
                String targetUserId = savedNotification.getUserId();
                dispatchService.dispatchNotification(targetUserId, response);
                sseMessagesSent++;
                if (event.getPriority() == com.example.notification.model.NotificationPriority.CRITICAL) {
                    log.info("Dispatching email for CRITICAL priority broadcast to user {} for notification ID {}. Event: {}", targetUserId, response.getId(), event);
                    dispatchService.dispatchToEmail(targetUserId, response);
                }
            } catch (Exception e) {
                log.error("Error converting or dispatching broadcast notification (DB ID: {}) to user {}: {}",
                          savedNotification.getId(), savedNotification.getUserId(), e.getMessage(), e);
            }
        }
        if (event.getPriority() == com.example.notification.model.NotificationPriority.CRITICAL) {
            log.info("CRITICAL priority broadcast: Attempted to dispatch emails to {} users.", users.size());
        }
        log.info("Broadcast notification processing complete. {} notifications created. {} sent via SSE.",
                  savedNotifications.size(), sseMessagesSent);
    }
}