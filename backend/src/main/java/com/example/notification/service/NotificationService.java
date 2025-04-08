package com.example.notification.service;

import com.example.notification.dto.NotificationEvent;
import com.example.notification.dto.NotificationResponse;
import com.example.notification.dto.NotificationStats;
import com.example.notification.model.Notification;
import com.example.notification.model.NotificationPriority;
import com.example.notification.model.NotificationStatus;
import com.example.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationProcessorService processorService;

    public Page<NotificationResponse> getUserNotifications(String userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::convertToResponse);
    }

    public Page<NotificationResponse> getNotificationsByType(String userId, String notificationType, Pageable pageable) {
        return notificationRepository.findByUserIdAndNotificationTypeOrderByCreatedAtDesc(userId, notificationType, pageable)
                .map(this::convertToResponse);
    }

    public Page<NotificationResponse> searchNotifications(String userId, String searchTerm, Pageable pageable) {
        return notificationRepository.searchNotifications(userId, searchTerm, pageable)
                .map(this::convertToResponse);
    }

    @Transactional
    public NotificationResponse sendBroadcastNotification(NotificationEvent event) {
        processorService.processBroadcastNotification(event);
        Notification broadcastNotification = Notification.builder()
                .userId("BROADCAST")
                .notificationType(event.getNotificationType())
                .priority(event.getPriority())
                .content(event.getContent())
                .readStatus(NotificationStatus.UNREAD)
                .sourceService(event.getSourceService())
                .metadata(event.getMetadata() != null ? serializeMetadata(event.getMetadata()) : null)
                .tags(event.getTags() != null ? String.join(",", event.getTags()) : null)
                .title(event.getTitle())
                .build();
        Notification saved = notificationRepository.save(broadcastNotification);
        return convertToResponse(saved);
    }

    @Transactional
    public NotificationResponse sendNotification(NotificationEvent event) {
        if (event.getTargetUserIds() == null || event.getTargetUserIds().isEmpty()) {
            throw new IllegalArgumentException("Please select at least one user to send the notification");
        }

        // Add detailed logging to debug the issue
        log.info("Received notification event: {}", event);
        log.info("Target users count: {}", event.getTargetUserIds().size());
        log.info("Target users: {}", event.getTargetUserIds());

        NotificationResponse response = null;
        for (String userId : event.getTargetUserIds()) {
            if (userId == null || userId.trim().isEmpty()) {
                log.warn("Skipping null or empty userId in targetUserIds");
                continue;
            }

            Notification notification = Notification.builder()
                    .userId(userId.trim())
                    .notificationType(event.getNotificationType())
                    .priority(event.getPriority())
                    .content(event.getContent())
                    .readStatus(NotificationStatus.UNREAD)
                    .sourceService(event.getSourceService())
                    .metadata(event.getMetadata() != null ? serializeMetadata(event.getMetadata()) : null)
                    .tags(event.getTags() != null ? String.join(",", event.getTags()) : null)
                    .title(event.getTitle())
                    .build();
            Notification saved = notificationRepository.save(notification);
            if (response == null) {
                response = convertToResponse(saved);
            }
        }
        return response;
    }

    private String serializeMetadata(Map<String, Object> metadata) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(metadata);
        } catch (Exception e) {
            return metadata.toString();
        }
    }

    public Page<NotificationResponse> getUnreadNotifications(String userId, Pageable pageable) {
        return notificationRepository.findByUserIdAndReadStatus(
                        userId, NotificationStatus.UNREAD, pageable)
                .map(this::convertToResponse);
    }

    public NotificationResponse getNotificationById(Long id) {
        return notificationRepository.findById(id)
                .map(this::convertToResponse)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
    }

    @Transactional
    public void markAsRead(Long id, String userId) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        if (notification.getUserId().equals(userId)) {
            notification.setReadStatus(NotificationStatus.READ);
            notificationRepository.save(notification);
        }
    }

    @Transactional
    public int markAllAsRead(String userId) {
        List<Notification> unreadNotifications = notificationRepository
                .findByUserIdAndReadStatus(userId, NotificationStatus.UNREAD, Pageable.unpaged())
                .getContent();
        unreadNotifications.forEach(n -> n.setReadStatus(NotificationStatus.READ));
        notificationRepository.saveAll(unreadNotifications);
        return unreadNotifications.size();
    }

    public NotificationStats getNotificationStats() {
        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);

        Map<String, Long> notificationsByType = notificationRepository.countGroupByNotificationType()
                .stream()
                .collect(Collectors.toMap(

                        row -> (String) row[0],
                        row -> (Long) row[1]
                ));

        Map<NotificationPriority, Long> notificationsByPriority = notificationRepository.countGroupByPriority()
                .stream()
                .collect(Collectors.toMap(
                        row -> (NotificationPriority) row[0],
                        row -> (Long) row[1]
                ));

        return NotificationStats.builder()
                .totalNotifications(notificationRepository.count())
                .unreadNotifications(notificationRepository.countByReadStatus(NotificationStatus.UNREAD))
                .criticalNotifications(notificationRepository.countByPriority(NotificationPriority.CRITICAL))
                .todayNotifications(notificationRepository.countByCreatedAtAfter(today))
                .notificationsByType(notificationsByType)
                .notificationsByPriority(notificationsByPriority)
                .build();
        }
    public List<NotificationResponse> getRecentNotifications(int limit) {
        Pageable pageable = org.springframework.data.domain.PageRequest.of(0, limit, org.springframework.data.domain.Sort.by("createdAt").descending());
        return notificationRepository.findAllByOrderByCreatedAtDesc(pageable)
                .getContent()
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    private NotificationResponse convertToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .notificationType(notification.getNotificationType())
                .priority(notification.getPriority())
                .content(notification.getContent())
                .readStatus(notification.getReadStatus())
                .sourceService(notification.getSourceService())
                .metadata(notification.getMetadata())
                .tags(notification.getTags())
                .title(notification.getTitle())
                .createdAt(notification.getCreatedAt())
                .build();
    }

    public long countUnreadNotifications(String userId) {
        return notificationRepository.countByUserIdAndReadStatus(userId, NotificationStatus.UNREAD);
    }
    public List<String> getNotificationTypes() {
        return notificationRepository.findDistinctNotificationTypes();
    }
}
