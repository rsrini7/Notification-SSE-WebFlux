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

    @Transactional
    public void processBroadcast(NotificationEvent event) {
        log.info("PROCESS_BROADCAST_START: eventId={}, title='{}'", event.getEventId(), event.getTitle()); // New log

        List<User> users = userRepository.findAll();
        if (users.isEmpty()) {
            log.info("No users found to broadcast notification to. EventId: {}", event.getEventId());
            return;
        }
        log.info("Found {} users for broadcast. EventId: {}", users.size(), event.getEventId()); // Existing log, ensure eventId

        log.info("PROCESS_BROADCAST_PERSIST: Attempting to persist notifications for eventId={}, userCount={}", event.getEventId(), users.size()); // New log
        List<Notification> savedNotifications = persistenceService.persistBroadcastNotifications(event, users);
        log.info("{} broadcast notifications successfully saved to the database. EventId: {}", savedNotifications.size(), event.getEventId()); // Existing log, ensure eventId

        int sseMessagesSent = 0;
        for (Notification savedNotification : savedNotifications) {
            try {
                NotificationResponse response = persistenceService.convertToResponse(savedNotification);
                String targetUserId = savedNotification.getUserId();

                dispatchService.dispatchNotification(targetUserId, response);
                sseMessagesSent++;

                if (event.getPriority() == com.example.notification.model.NotificationPriority.CRITICAL) {
                    log.info("PROCESS_BROADCAST_EMAIL_DISPATCH: Attempting email dispatch for eventId={}, userId={}, notificationId={}", event.getEventId(), targetUserId, response.getId()); // New log
                    dispatchService.dispatchToEmail(targetUserId, response);
                }
            } catch (Exception e) {
                log.error("Error in broadcast processing loop for eventId={}, userId={}: {}",
                          event.getEventId(), savedNotification.getUserId(), e.getMessage(), e); // Enhanced existing error log
            }
        }
        if (event.getPriority() == com.example.notification.model.NotificationPriority.CRITICAL) {
            log.info("CRITICAL priority broadcast: Attempted to dispatch emails to {} users. EventId: {}", users.size(), event.getEventId());
        }
        log.info("PROCESS_BROADCAST_END: eventId={}. {} notifications created. {} sent via SSE.",
                 event.getEventId(), savedNotifications.size(), sseMessagesSent); // Enhanced existing log
    }
}