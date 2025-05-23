package com.example.notification.service;

import com.example.notification.dto.NotificationEvent;
import com.example.notification.dto.NotificationResponse;
import com.example.notification.model.Notification;
import com.example.notification.model.NotificationStatus;
import com.example.notification.model.User;
import com.example.notification.repository.NotificationRepository;
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
            WebSocketSessionManager webSocketSessionManager,
            UserRepository userRepository,
            EmailService emailService,
            ObjectMapper objectMapper) {
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

        // Fetch all users
        List<User> users = userRepository.findAll();
        List<Notification> notifications = new ArrayList<>();
        for (User user : users) {
            Notification userNotification = Notification.builder()
                .userId(user.getUsername())
                .sourceService(event.getSourceService())
                .notificationType(event.getNotificationType())
                .priority(event.getPriority())
                .content(event.getContent())
                .metadata(serializeToJson(event.getMetadata()))
                .tags(serializeToJson(event.getTags()))
                .readStatus(NotificationStatus.UNREAD)
                .title(event.getTitle()) // Ensure title is set from the event
                .build();
            notifications.add(userNotification);
        }
        List<Notification> savedNotifications = notificationRepository.saveAll(notifications);

        // Optionally, send to all connected users via WebSocket
        for (Notification saved : savedNotifications) {
            NotificationResponse response = convertToResponse(saved);
            webSocketSessionManager.sendToUser(saved.getUserId(), userNotificationsDestination, response);
        }
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
        return Notification.builder()
                .userId(userId)
                .sourceService(event.getSourceService())
                .notificationType(event.getNotificationType())
                .priority(event.getPriority())
                .content(event.getContent())
                .metadata(serializeToJson(event.getMetadata()))
                .tags(serializeToJson(event.getTags()))
                .readStatus(NotificationStatus.UNREAD)
                .title(event.getTitle()) // Ensure title is set from the event
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
                .notificationType(notification.getNotificationType())
                .priority(notification.getPriority())
                .content(notification.getContent())
                .metadata(deserializeFromJson(notification.getMetadata()))
                .tags(deserializeFromJson(notification.getTags()))
                .createdAt(notification.getCreatedAt())
                .readStatus(notification.getReadStatus())
                .title(notification.getTitle()) // Ensure title is mapped to the response
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