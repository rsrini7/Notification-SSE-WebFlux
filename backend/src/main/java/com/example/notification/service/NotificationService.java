package com.example.notification.service;

import com.example.notification.dto.NotificationEvent;
import com.example.notification.dto.NotificationResponse;
import com.example.notification.model.Notification;
import com.example.notification.model.NotificationStatus;
import com.example.notification.repository.NotificationRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for retrieving and managing notifications
 */
@Service
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;
    private final UserService userService;
    private final String broadcastNotificationsTopic;

    public NotificationService(
            NotificationRepository notificationRepository,
            ObjectMapper objectMapper,
            KafkaTemplate<String, NotificationEvent> kafkaTemplate,
            UserService userService,
            @Value("${notification.kafka.topics.broadcast-notifications}") String broadcastNotificationsTopic) {
        this.notificationRepository = notificationRepository;
        this.objectMapper = objectMapper;
        this.kafkaTemplate = kafkaTemplate;
        this.userService = userService;
        this.broadcastNotificationsTopic = broadcastNotificationsTopic;
    }

    /**
     * Get notifications for a user with pagination
     */
    public Page<NotificationResponse> getNotificationsForUser(String userId, Pageable pageable) {
        log.info("Getting notifications for user: {}", userId);
        Page<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return notifications.map(this::convertToResponse);
    }

    /**
     * Get unread notifications for a user with pagination
     */
    public Page<NotificationResponse> getUnreadNotificationsForUser(String userId, Pageable pageable) {
        log.info("Getting unread notifications for user: {}", userId);
        Page<Notification> notifications = notificationRepository.findByUserIdAndReadStatus(
                userId, NotificationStatus.UNREAD, pageable);
        return notifications.map(this::convertToResponse);
    }

    /**
     * Get notifications by type for a user with pagination
     */
    public Page<NotificationResponse> getNotificationsByType(String userId, String notificationType, Pageable pageable) {
        log.info("Getting notifications of type {} for user: {}", notificationType, userId);
        Page<Notification> notifications = notificationRepository.findByUserIdAndNotificationType(
                userId, notificationType, pageable);
        return notifications.map(this::convertToResponse);
    }

    /**
     * Search notifications by content for a user with pagination
     */
    public Page<NotificationResponse> searchNotifications(String userId, String searchTerm, Pageable pageable) {
        log.info("Searching notifications with term '{}' for user: {}", searchTerm, userId);
        Page<Notification> notifications = notificationRepository.searchByContent(userId, searchTerm, pageable);
        return notifications.map(this::convertToResponse);
    }

    /**
     * Get a notification by ID
     */
    public Optional<NotificationResponse> getNotificationById(Long id) {
        log.info("Getting notification by ID: {}", id);
        return notificationRepository.findById(id).map(this::convertToResponse);
    }

    /**
     * Mark a notification as read
     */
    @Transactional
    public boolean markNotificationAsRead(Long id, String userId) {
        log.info("Marking notification {} as read for user: {}", id, userId);
        Optional<Notification> notificationOpt = notificationRepository.findById(id);
        
        if (notificationOpt.isPresent()) {
            Notification notification = notificationOpt.get();
            
            // Ensure the notification belongs to the user
            if (!notification.getUserId().equals(userId)) {
                log.warn("User {} attempted to mark notification {} as read, but it belongs to user {}",
                        userId, id, notification.getUserId());
                return false;
            }
            
            notification.setReadStatus(NotificationStatus.READ);
            notificationRepository.save(notification);
            return true;
        }
        
        return false;
    }

    /**
     * Mark all notifications as read for a user
     */
    @Transactional
    public int markAllNotificationsAsRead(String userId) {
        log.info("Marking all notifications as read for user: {}", userId);
        return notificationRepository.updateReadStatusForUser(userId, NotificationStatus.READ);
    }

    /**
     * Count unread notifications for a user
     */
    public long countUnreadNotifications(String userId) {
        return notificationRepository.countByUserIdAndReadStatus(userId, NotificationStatus.UNREAD);
    }

    /**
     * Send a broadcast notification
     */
    public void sendBroadcastNotification(NotificationEvent event) {
        log.info("Sending broadcast notification: {}", event);
        kafkaTemplate.send(broadcastNotificationsTopic, event);
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
                .build();
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