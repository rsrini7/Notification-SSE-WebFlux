package com.example.notification.service;

import com.example.notification.dto.NotificationEvent;
import com.example.notification.dto.NotificationResponse;
import com.example.notification.model.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Important for atomicity

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationProcessingOrchestrator {

    private final NotificationPersistenceService persistenceService;
    private final NotificationDispatchService dispatchService;

    /**
     * Process a standard or critical notification for specific users.
     * @param event The notification event.
     * @param isCritical Whether this is a critical notification.
     */
    @Transactional // Ensures that saving and dispatch attempts are part of the same transaction context
    public void processNotification(NotificationEvent event, boolean isCritical) {
        log.info("Orchestrating processing for {} notification: {}", isCritical ? "critical" : "standard", event);

        validateNotificationEvent(event);

        // Handle broadcast use case: targetUserIds contains only "ALL"
        java.util.List<String> targetUserIds = event.getTargetUserIds();
        if (targetUserIds == null || targetUserIds.isEmpty()) {
            log.warn("No target user IDs provided for notification. Skipping. Event: {}", event);
            return;
        }

        if (targetUserIds.size() == 1 && "ALL".equalsIgnoreCase(targetUserIds.get(0))) {
            // Fetch all user IDs from persistenceService or user service
            try {
                targetUserIds = persistenceService.getAllUserIds();
                if (targetUserIds == null || targetUserIds.isEmpty()) {
                    log.warn("No users found for broadcast notification. Event: {}", event);
                    return;
                }
                log.info("Broadcasting notification to all users ({} total)", targetUserIds.size());
            } catch (Exception e) {
                log.error("Failed to fetch all user IDs for broadcast: {}", e.getMessage(), e);
                return;
            }
        }

        int processedCount = 0;
        for (String userId : targetUserIds) {
            if (userId == null || userId.trim().isEmpty()) {
                log.warn("Skipping notification for null or empty userId in event: {}", event);
                continue;
            }
            try {
                // 1. Persist the notification
                Notification savedNotification = persistenceService.persistNotification(event, userId);

                // 2. Convert to response DTO
                NotificationResponse response = persistenceService.convertToResponse(savedNotification);

                // 3. Dispatch via SSE (and other channels if applicable)
                dispatchService.dispatchNotification(userId, response);

                // 4. Dispatch via Email if critical
                if (isCritical) {
                    dispatchService.dispatchToEmail(userId, response);
                }
                processedCount++;
            } catch (Exception e) {
                log.error("Error processing notification for user {}: {}. Event: {}", userId, e.getMessage(), event, e);
                // Depending on requirements, you might rethrow, or collect errors, or send to DLQ
            }
        }
        log.info("Successfully processed notification for {} target users.", processedCount);
    }

    private void validateNotificationEvent(NotificationEvent event) {
        // Basic validation, can be expanded or moved to a dedicated validation service
        if (event == null) {
            throw new IllegalArgumentException("Notification event cannot be null");
        }
        // For broadcast, targetUserIds might be null/empty and handled by BroadcastService.
        // For specific notifications, targetUserIds must be present.
        // This check is now before the loop in processNotification.

        if (event.getSourceService() == null || event.getSourceService().isEmpty()) {
            throw new IllegalArgumentException("Source service cannot be empty");
        }
        if (event.getNotificationType() == null || event.getNotificationType().isEmpty()) {
            throw new IllegalArgumentException("Notification type cannot be empty");
        }
        if (event.getContent() == null || event.getContent().isEmpty()) {
            throw new IllegalArgumentException("Content cannot be empty");
        }
        if (event.getPriority() == null) {
             throw new IllegalArgumentException("Priority cannot be null");
        }
    }
}