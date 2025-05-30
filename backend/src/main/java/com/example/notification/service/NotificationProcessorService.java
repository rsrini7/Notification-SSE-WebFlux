package com.example.notification.service;

import com.example.notification.dto.NotificationEvent;
import com.example.notification.dto.NotificationResponse;
import com.example.notification.model.Notification;
import com.example.notification.model.NotificationStatus;
import com.example.notification.model.NotificationType;
import com.example.notification.model.User;
import com.example.notification.repository.NotificationRepository;
import com.example.notification.repository.NotificationTypeRepository;
import com.example.notification.repository.UserRepository;
import com.example.notification.websocket.WebSocketSessionManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
// Removed HashMap as it's no longer used in processBroadcastNotification
import java.util.List;
import java.util.Map; // Retained for deserializeFromJson, though it could be more specific if only Maps are expected

/**
 * Core service for processing notifications
 * Handles validation, persistence, and dispatching through appropriate channels
 */
@Service
@Slf4j
public class NotificationProcessorService {

    private final NotificationRepository notificationRepository;
    private final NotificationTypeRepository notificationTypeRepository;
    private final WebSocketSessionManager webSocketSessionManager;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    @Value("${notification.websocket.user-notifications-destination}")
    private String userNotificationsDestination;

    @Value("${notification.websocket.broadcast-destination}")
    private String broadcastDestination; // This is no longer used for sending, but retained if needed elsewhere

    public NotificationProcessorService(
            NotificationRepository notificationRepository,
            NotificationTypeRepository notificationTypeRepository,
            WebSocketSessionManager webSocketSessionManager,
            UserRepository userRepository,
            EmailService emailService,
            ObjectMapper objectMapper) {
        this.notificationTypeRepository = notificationTypeRepository;
        this.notificationRepository = notificationRepository;
        this.webSocketSessionManager = webSocketSessionManager;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.objectMapper = objectMapper;
    }

    /**
     * Process a notification for specific users
     * @param event The notification event from Kafka
     * @param isCritical Whether this is a critical notification that may require email delivery
     */
    @Transactional
    public void processNotification(NotificationEvent event, boolean isCritical) {
        log.info("Processing {} notification: {}", isCritical ? "critical" : "standard", event);

        // Validate the event
        validateNotificationEvent(event);

        List<Notification> savedNotifications = new ArrayList<>();

        // Process for each target user
        for (String userId : event.getTargetUserIds()) {
            if (userId == null) {
                log.warn("Skipping notification for null userId");
                continue;
            }
            // Create and save notification entity
            Notification notification = createNotificationEntity(event, userId);
            Notification savedNotification = notificationRepository.save(notification);
            savedNotifications.add(savedNotification);

            // Convert to response DTO
            NotificationResponse response = convertToResponse(savedNotification);

            // Send via WebSocket if user is connected
            if (webSocketSessionManager.isUserConnected(userId)) {
                webSocketSessionManager.sendToUser(userId, userNotificationsDestination, response);
                log.debug("Sent notification to user {} via WebSocket with ID {}", userId, response.getId());
            } else {
                log.debug("User {} not connected via WebSocket", userId);
            }

            // Send email for critical notifications
            if (isCritical) {
                emailService.sendNotificationEmail(userId, response);
                log.debug("Sent critical notification to user {} via email", userId);
            }
        }

        log.info("Successfully processed notification for {} users", savedNotifications.size());
    }

    /**
     * Process a broadcast notification to all users
     * @param event The broadcast notification event from Kafka
     */
    @Transactional
    public void processBroadcastNotification(NotificationEvent event) {
        log.info("Processing broadcast notification event: {}", event);

        // Find or create the notification type
        NotificationType notificationType = notificationTypeRepository.findByTypeCode(event.getNotificationType())
                .orElseGet(() -> {
                    log.info("Notification type {} not found, creating new type.", event.getNotificationType());
                    NotificationType newType = new NotificationType();
                    newType.setTypeCode(event.getNotificationType());
                    newType.setDescription("Automatically created for " + event.getNotificationType());
                    return notificationTypeRepository.save(newType);
                });

        List<User> users = userRepository.findAll();
        if (users.isEmpty()) {
            log.info("No users found to broadcast notification to.");
            return;
        }

        log.info("Found {} users to create broadcast notifications for.", users.size());

        List<Notification> notificationsToSave = new ArrayList<>();
        for (User user : users) {
            Notification userNotification = Notification.builder()
                    .userId(user.getUsername()) 
                    .sourceService(event.getSourceService())
                    .notificationType(notificationType)
                    .priority(event.getPriority())
                    .content(event.getContent())
                    .metadata(serializeToJson(event.getMetadata()))
                    .tags(serializeToJson(event.getTags()))
                    .readStatus(NotificationStatus.UNREAD)
                    .title(event.getTitle())
                    .build();
            notificationsToSave.add(userNotification);
        }

        List<Notification> savedNotifications = notificationRepository.saveAll(notificationsToSave);
        log.info("{} broadcast notifications successfully saved to the database.", savedNotifications.size());

        int webSocketMessagesSent = 0;
        for (Notification savedNotification : savedNotifications) {
            try {
                NotificationResponse response = convertToResponse(savedNotification);
                String targetUserId = savedNotification.getUserId();

                if (webSocketSessionManager.isUserConnected(targetUserId)) {
                    webSocketSessionManager.sendToUser(targetUserId, userNotificationsDestination, response);
                    log.info("Sent broadcast notification (DB ID: {}) via WebSocket to user {}.", savedNotification.getId(), targetUserId);
                    webSocketMessagesSent++;
                } else {
                    log.debug("User {} not connected via WebSocket, broadcast notification (DB ID: {}) was saved but not sent in real-time.", targetUserId, savedNotification.getId());
                }
            } catch (Exception e) {
                log.error("Error converting or sending broadcast notification (DB ID: {}) to user {}: {}", 
                          savedNotification.getId(), savedNotification.getUserId(), e.getMessage(), e);
            }
        }
        log.info("Broadcast notification processing complete. {} notifications created. {} notifications sent via WebSocket.",
                  savedNotifications.size(), webSocketMessagesSent);
    }


    /**
     * Validate the notification event
     */
    private void validateNotificationEvent(NotificationEvent event) {
        // For broadcast, targetUserIds might be null or empty, which is acceptable.
        // The original validation is more for specific user notifications.
        // We can adjust if broadcast events have different validation rules.
        if (event.getSourceService() == null || event.getSourceService().isEmpty()) {
            throw new IllegalArgumentException("Source service cannot be empty");
        }
        if (event.getNotificationType() == null || event.getNotificationType().isEmpty()) {
            throw new IllegalArgumentException("Notification type cannot be empty");
        }
        if (event.getContent() == null || event.getContent().isEmpty()) {
            throw new IllegalArgumentException("Content cannot be empty");
        }
        // Priority is an enum, so it can't be null if properly deserialized
    }

    /**
     * Create a notification entity from an event
     */
    private Notification createNotificationEntity(NotificationEvent event, String userId) {
        // Find or create the notification type
        NotificationType notificationType = notificationTypeRepository.findByTypeCode(event.getNotificationType())
                .orElseGet(() -> {
                    // If type doesn't exist, create a new one
                    NotificationType newType = new NotificationType();
                    newType.setTypeCode(event.getNotificationType());
                    newType.setDescription("Automatically created for " + event.getNotificationType());
                    return notificationTypeRepository.save(newType);
                });

        return Notification.builder()
                .userId(userId)
                .sourceService(event.getSourceService())
                .notificationType(notificationType)
                .priority(event.getPriority())
                .content(event.getContent())
                .metadata(serializeToJson(event.getMetadata()))
                .tags(serializeToJson(event.getTags()))
                .readStatus(NotificationStatus.UNREAD)
                .title(event.getTitle())
                .build();
    }

    /**
     * Convert a notification entity to a response DTO
     */
    private NotificationResponse convertToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId()) // This is the crucial Long ID
                .userId(notification.getUserId())
                .sourceService(notification.getSourceService())
                .notificationType(notification.getNotificationType() != null ? notification.getNotificationType().getTypeCode() : null)
                .priority(notification.getPriority())
                .content(notification.getContent())
                .metadata(deserializeFromJson(notification.getMetadata()))
                .tags(deserializeFromJson(notification.getTags()))
                .createdAt(notification.getCreatedAt())
                .readStatus(notification.getReadStatus())
                .title(notification.getTitle())
                .build();
    }

    /**
     * Serialize an object to JSON string
     */
    private String serializeToJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Error serializing object to JSON: {}", e.getMessage(), e);
            // Consider rethrowing as a runtime exception or returning a specific error indicator
            return null;
        }
    }

    /**
     * Deserialize a JSON string to an object (Map or List typically)
     */
    @SuppressWarnings("unchecked")
    private <T> T deserializeFromJson(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            // This will deserialize to a Map<String, Object> or List<Object> by default
            return (T) objectMapper.readValue(json, Object.class);
        } catch (JsonProcessingException e) {
            log.error("Error deserializing JSON to object: {}", e.getMessage(), e);
            // Consider rethrowing as a runtime exception or returning a specific error indicator
            return null;
        }
    }
}