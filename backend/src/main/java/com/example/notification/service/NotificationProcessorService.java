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

import java.util.ArrayList;
import java.util.List;

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
    private String broadcastDestination;

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
                log.debug("Sent notification to user {} via WebSocket", userId);
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
        log.info("Processing broadcast notification: {}", event);

        // *** START OF KEY CHANGES FOR BROADCAST PAYLOAD ***
        java.util.Map<String, Object> payloadMap = new java.util.HashMap<>();
        payloadMap.put("sourceService", event.getSourceService());
        payloadMap.put("notificationType", event.getNotificationType());
        payloadMap.put("priority", event.getPriority());
        payloadMap.put("content", event.getContent());
        if (event.getMetadata() != null) {
            payloadMap.put("metadata", event.getMetadata());
        }
        if (event.getTags() != null) {
            payloadMap.put("tags", event.getTags());
        }
        if (event.getTitle() != null) {
            payloadMap.put("title", event.getTitle());
        }
        payloadMap.put("createdAt", java.time.LocalDateTime.now().toString()); // Ensure ISO 8601 format
        // *** END OF KEY CHANGES FOR BROADCAST PAYLOAD ***

        String contentSnippet = event.getContent() != null ? event.getContent().substring(0, Math.min(event.getContent().length(), 100)) : "null";
        // Ensure logging uses payloadMap if that's what's sent
        log.info("Attempting to send broadcast via WebSocket. Destination: '{}', Payload Type: '{}', Content Snippet: '{}'", broadcastDestination, payloadMap.get("notificationType"), contentSnippet);
        webSocketSessionManager.sendBroadcast(broadcastDestination, payloadMap); // Send the payloadMap
        log.info("Broadcast message processing invoked for WebSocket destination: '{}'. Payload Type: '{}'", broadcastDestination, payloadMap.get("notificationType"));

        // ... (rest of the method for saving notifications remains the same) ...
        NotificationType notificationType = notificationTypeRepository.findByTypeCode(event.getNotificationType())
                .orElseGet(() -> {
                    NotificationType newType = new NotificationType();
                    newType.setTypeCode(event.getNotificationType());
                    newType.setDescription("Automatically created for " + event.getNotificationType());
                    return notificationTypeRepository.save(newType);
                });

        // Fetch all users and create notifications
        List<User> users = userRepository.findAll();
        List<Notification> notifications = new ArrayList<>();
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
                // Note: The 'createdAt' for the persisted Notification entity will be set by JPA/database
                .build();
            notifications.add(userNotification);
        }
        List<Notification> savedNotifications = notificationRepository.saveAll(notifications);
        log.info("Broadcast notification sent to all users ({} notifications created)", savedNotifications.size());
    }

    /**
     * Validate the notification event
     */
    private void validateNotificationEvent(NotificationEvent event) {
        if (event.getTargetUserIds() == null || event.getTargetUserIds().isEmpty()) {
            throw new IllegalArgumentException("Target user IDs cannot be empty");
        }
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
                .id(notification.getId())
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
            return null;
        }
    }

    /**
     * Deserialize a JSON string to an object
     */
    @SuppressWarnings("unchecked")
    private <T> T deserializeFromJson(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return (T) objectMapper.readValue(json, Object.class);
        } catch (JsonProcessingException e) {
            log.error("Error deserializing JSON to object: {}", e.getMessage(), e);
            return null;
        }
    }
}