package com.example.notification.service;

import com.example.notification.dto.NotificationEvent;
import com.example.notification.dto.NotificationResponse;
import com.example.notification.dto.NotificationStats;
import com.example.notification.model.Notification;
import com.example.notification.model.NotificationPriority;
import com.example.notification.model.NotificationStatus;
import com.example.notification.model.NotificationType;
import com.example.notification.repository.NotificationRepository;
import com.example.notification.repository.NotificationTypeRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.stream.Collectors;

@Service
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationTypeRepository notificationTypeRepository;
    private final NotificationProcessingOrchestrator notificationProcessingOrchestrator;
    private final SseEmitterManager sseEmitterManager;
    private final ObjectMapper objectMapper;

    public NotificationService(NotificationRepository notificationRepository,
                               NotificationTypeRepository notificationTypeRepository,
                               NotificationProcessingOrchestrator notificationProcessingOrchestrator,
                               SseEmitterManager sseEmitterManager,
                               ObjectMapper objectMapper) {
        this.notificationRepository = notificationRepository;
        this.notificationTypeRepository = notificationTypeRepository;
        this.notificationProcessingOrchestrator = notificationProcessingOrchestrator;
        this.sseEmitterManager = sseEmitterManager;
        this.objectMapper = objectMapper;
    }

    public Page<NotificationResponse> getUserNotifications(String userId, Pageable pageable) {
        log.info("Fetching notifications for userId={}, pageable={}", userId, pageable);
        Page<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        log.info("Fetched {} notifications for userId={}", notifications.getTotalElements(), userId);
        if (notifications.getContent().isEmpty()) {
            log.warn("No notifications found for userId={}", userId);
        } else {
            notifications.getContent().forEach(n ->
                log.debug("Notification: id={}, type={}, content={}, createdAt={}",
                    n.getId(), n.getNotificationType(), n.getContent(), n.getCreatedAt())
            );
        }
        return notifications.map(this::convertToResponse);
    }

    public Page<NotificationResponse> getNotificationsByType(String userId, String notificationType, Pageable pageable) {
        return notificationRepository.findByUserIdAndNotificationTypeOrderByCreatedAtDesc(userId, notificationType, pageable)
                .map(this::convertToResponse);
    }

    /**
     * Escapes characters that are special in H2's REGEXP_LIKE.
     * Special characters: . \ ( ) [ ] { } * + ? | ^ $
     * They need to be escaped with a backslash.
     * @param str The string to escape.
     * @return The escaped string.
     */
    private String escapeRegexChars(String str) {
        // The replacement string \\\\$1 means:
        // First \\ for Java string literal backslash,
        // then \\ for regex literal backslash for the engine,
        // then $1 for the captured group (the special character itself).
        return str.replaceAll("([.\\\\()\\[\\]{}*+?|^$])", "\\\\$1");
    }

    public Page<NotificationResponse> searchNotifications(String userId, String searchTerm, Pageable pageable) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return Page.empty(pageable);
        }
    
        String[] words = searchTerm.trim().split("\\s+");
        if (words.length == 0) {
            return Page.empty(pageable);
        }
    
        String searchTermRegex = Arrays.stream(words)
                                   .filter(word -> !word.isEmpty())
                                   .map(this::escapeRegexChars)
                                   .collect(Collectors.joining("|"));
        
        if (searchTermRegex.isEmpty()) {
            return Page.empty(pageable);
        }
        log.debug("Searching notifications for userId {} with regex: {}", userId, searchTermRegex);
        return notificationRepository.searchNotifications(userId, searchTermRegex, pageable)
                .map(this::convertToResponse);
    }

    @Transactional
    public void sendBroadcastNotification(NotificationEvent event) { // Return type changed to void
        notificationProcessingOrchestrator.processBroadcastNotification(event);
        // No need to create a BROADCAST notification record; per-user notifications are created in notificationProcessingOrchestrator
        // Return null was removed
    }

    @Transactional
    public void sendNotification(NotificationEvent event) { // Return type changed to void
        if (event.getTargetUserIds() == null || event.getTargetUserIds().isEmpty()) {
            throw new IllegalArgumentException("Please select at least one user to send the notification");
        }

        // Add detailed logging to debug the issue
        log.info("Received notification event: {}", event);
        log.info("Target users count: {}", event.getTargetUserIds().size());
        log.info("Target users: {}", event.getTargetUserIds());
        log.info("Dispatching to NotificationProcessingOrchestrator. Critical: {}", event.isCritical());

        // Delegate to the orchestrator
        notificationProcessingOrchestrator.processNotification(event, event.isCritical());
        // The orchestrator will handle type creation, persistence, and SSE
    }

    private String serializeObjectToJson(Object data) {
        if (data == null) {
            return null;
        }
        try {
            return this.objectMapper.writeValueAsString(data);
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
            return (T) this.objectMapper.readValue(json, Object.class);
        } catch (JsonProcessingException e) {
            log.error("Error deserializing JSON to object: {}", e.getMessage(), e);
            return null;
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
                .filter(row -> row[0] != null)
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (Long) row[1]
                ));

        Map<NotificationPriority, Long> notificationsByPriority = notificationRepository.countGroupByPriority()
                .stream()
                .filter(row -> row[0] != null)
                .collect(Collectors.toMap(
                        row -> (NotificationPriority) row[0],
                        row -> (Long) row[1]
                ));

        long total = notificationRepository.count();
        long unread = notificationRepository.countByReadStatus(NotificationStatus.UNREAD);
        Double readRate = (total > 0) ? ((total - unread) * 100.0 / total) : null;

        NotificationStats stats = NotificationStats.builder()
        .totalNotifications(total)
        .unreadNotifications(unread)
        .criticalNotifications(notificationRepository.countByPriority(NotificationPriority.CRITICAL))
        .todayNotifications(notificationRepository.countByCreatedAtAfter(today))
        .notificationsByType(notificationsByType)
        .notificationsByPriority(notificationsByPriority)
        .readRate(readRate)
        .build();
        log.info("Returning NotificationStats: {}", stats);
        return stats;
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

    public long countUnreadNotifications(String userId) {
        return notificationRepository.countByUserIdAndReadStatus(userId, NotificationStatus.UNREAD);
    }
    public List<String> getNotificationTypes() {
        return notificationTypeRepository.findByActiveTrue().stream()
                .map(NotificationType::getTypeCode)
                .collect(Collectors.toList());
    }
}