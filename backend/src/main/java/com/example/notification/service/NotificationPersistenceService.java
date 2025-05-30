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
        NotificationType notificationType = findOrCreateNotificationType(event.getNotificationType());

        Notification notification = Notification.builder()
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
        return notificationRepository.save(notification);
    }

    @Transactional
    public List<Notification> persistBroadcastNotifications(NotificationEvent event, List<User> users) {
        NotificationType notificationType = findOrCreateNotificationType(event.getNotificationType());

        List<Notification> notificationsToSave = users.stream()
                .map(user -> Notification.builder()
                        .userId(user.getUsername())
                        .sourceService(event.getSourceService())
                        .notificationType(notificationType)
                        .priority(event.getPriority())
                        .content(event.getContent())
                        .metadata(serializeToJson(event.getMetadata()))
                        .tags(serializeToJson(event.getTags()))
                        .readStatus(NotificationStatus.UNREAD)
                        .title(event.getTitle())
                        .build())
                .collect(Collectors.toList());

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