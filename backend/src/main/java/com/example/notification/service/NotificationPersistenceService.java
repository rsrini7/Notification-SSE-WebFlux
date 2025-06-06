package com.example.notification.service;

import com.example.notification.dto.NotificationResponse;
import com.example.notification.model.Notification;
import com.example.notification.model.NotificationStatus;
import com.example.notification.model.NotificationType;
import com.example.notification.model.User;
import com.example.notification.repository.NotificationRepository;
import com.example.notification.repository.NotificationTypeRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.notification.dto.NotificationEvent;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationPersistenceService {

    private final NotificationRepository notificationRepository;
    private final NotificationTypeRepository notificationTypeRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public Notification persistNotification(NotificationEvent event, String userId) {
        // Validate eventId
        if (event.getEventId() == null || event.getEventId().trim().isEmpty()) {
            log.error("eventId is mandatory and cannot be null or empty for NotificationEvent. Event: {}", event);
            throw new IllegalArgumentException("eventId is mandatory and cannot be null or empty for notifications.");
        }

        // Check for existing notification if eventId is present
        java.util.Optional<Notification> existingNotification = notificationRepository.findByEventIdAndUserId(event.getEventId(), userId);
        if (existingNotification.isPresent()) {
            log.info("Notification with eventId {} and userId {} already exists with ID {}. Skipping persistence.",
                     event.getEventId(), userId, existingNotification.get().getId());
            return existingNotification.get();
        }

        NotificationType notificationType = findOrCreateNotificationType(event.getNotificationType());

        Notification notification = Notification.builder()
                .userId(userId)
                .eventId(event.getEventId()) // Set eventId
                .sourceService(event.getSourceService())
                .notificationType(notificationType)
                .priority(event.getPriority())
                .content(event.getContent())
                .metadata(serializeToJson(event.getMetadata()))
                .tags(serializeToJson(event.getTags()))
                .readStatus(NotificationStatus.UNREAD)
                .title(event.getTitle())
                .build();
        return notificationRepository.save(notification);
    }

    @Transactional
    public List<Notification> persistBroadcastNotifications(NotificationEvent event, List<User> users) {
        // Note: Idempotency for broadcast notifications is more complex.
        // If an eventId is present, it means the event itself is unique.
        // Persisting for each user means each user-notification will be unique by (eventId, userId).
        // The current NotificationRepository.findByEventIdAndUserId would handle this if called per user.
        // However, saveAll doesn't typically do individual checks.
        // For true idempotency on broadcast, we might need to filter users for whom the (eventId, userId) combo already exists.
        // This implementation will rely on the unique constraint (eventId, userId) at DB level if saveAll attempts duplicates with same eventId.
        // Or, iterate and call persistNotification for each user if strict per-user idempotency check before attempting save is needed.

        if (event.getEventId() == null || event.getEventId().trim().isEmpty()) {
            log.error("eventId is mandatory and cannot be null or empty for broadcast NotificationEvent. Event: {}", event);
            throw new IllegalArgumentException("eventId is mandatory and cannot be null or empty for broadcast notifications.");
        }

        NotificationType notificationType = findOrCreateNotificationType(event.getNotificationType());

        List<Notification> notificationsToSave = users.stream()
                .map(user -> {
                    // Optional: Add individual idempotency check here if needed before adding to batch
                    // if (event.getEventId() != null && !event.getEventId().trim().isEmpty()) {
                    //     Optional<Notification> existing = notificationRepository.findByEventIdAndUserId(event.getEventId(), user.getUsername());
                    //     if (existing.isPresent()) {
                    //         log.info("Broadcast notification for eventId {} and userId {} already exists. Skipping for this user.", event.getEventId(), user.getUsername());
                    //         return null; // Will be filtered out by .filter(Objects::nonNull)
                    //     }
                    // }
                    return Notification.builder()
                            .userId(user.getUsername())
                            .eventId(event.getEventId()) // Set eventId
                            .sourceService(event.getSourceService())
                            .notificationType(notificationType)
                            .priority(event.getPriority())
                            .content(event.getContent())
                            .metadata(serializeToJson(event.getMetadata()))
                            .tags(serializeToJson(event.getTags()))
                            .readStatus(NotificationStatus.UNREAD)
                            .title(event.getTitle())
                            .build();
                })
                // .filter(java.util.Objects::nonNull) // Uncomment if individual check above is used
                .collect(Collectors.toList());

        if (notificationsToSave.isEmpty()) {
            log.info("No new broadcast notifications to save for event: {}", event.getEventId());
            return java.util.Collections.emptyList();
        }

        return notificationRepository.saveAll(notificationsToSave);
    }

    private NotificationType findOrCreateNotificationType(String typeCode) {
        return notificationTypeRepository.findByTypeCode(typeCode)
                .orElseGet(() -> {
                    log.info("Notification type {} not found, creating new type.", typeCode);
                    NotificationType newType = new NotificationType();
                    newType.setTypeCode(typeCode);
                    newType.setDescription("Automatically created for " + typeCode);
                    return notificationTypeRepository.save(newType);
                });
    }

    public NotificationResponse convertToResponse(Notification notification) {
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