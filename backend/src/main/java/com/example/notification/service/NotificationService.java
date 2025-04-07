package com.example.notification.service;

import com.example.notification.dto.*;
import com.example.notification.model.*;
import com.example.notification.repository.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest; // Import PageRequest
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final NotificationProcessorService processorService;
    // Removed UserService dependency to break circular dependency
    // private final UserService userService;

    public Page<NotificationResponse> getUserNotifications(String userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::convertToResponse);
    }

    public Page<NotificationResponse> getNotificationsByType(String userId, String notificationType, Pageable pageable) {
        return notificationRepository.findByUserIdAndNotificationType(userId, notificationType, pageable)
                .map(this::convertToResponse);
    }

    public Page<NotificationResponse> searchNotifications(String userId, String searchTerm, Pageable pageable) {
        return notificationRepository.searchNotifications(userId, searchTerm, pageable)
                .map(this::convertToResponse);
    }

    public List<NotificationResponse> getRecentNotifications(int limit) {
        // Use PageRequest to limit results with the new repository method
        return notificationRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit))
                .getContent() // Get the List<Notification> from the Page
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public NotificationResponse sendBroadcastNotification(NotificationEvent event) {
        processorService.processBroadcastNotification(event);
        // Optionally save a broadcast notification record
        Notification broadcastNotification = Notification.builder()
                .userId("BROADCAST")
                .notificationType(event.getNotificationType())
                .priority(event.getPriority())
                .content(event.getContent())
                .readStatus(NotificationStatus.UNREAD)
                .build();
        Notification saved = notificationRepository.save(broadcastNotification);
        return convertToResponse(saved);
    }

    public Page<NotificationResponse> getUnreadNotifications(String userId, Pageable pageable) {
        return notificationRepository.findByUserIdAndReadStatus(
                userId, NotificationStatus.UNREAD, pageable)
                .map(this::convertToResponse);
    }

    public NotificationResponse getNotificationById(Long id) {
        return notificationRepository.findById(id)
                .map(this::convertToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
    }

    @Transactional
    public void markAsRead(Long id, String userId) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
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

    public long countUnreadNotifications(String userId) {
        return notificationRepository.countByUserIdAndReadStatus(userId, NotificationStatus.UNREAD);
    }

    public NotificationStats getNotificationStats() {
        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        
        return NotificationStats.builder()
                .totalNotifications(notificationRepository.count())
                .unreadNotifications(notificationRepository.countByReadStatus(NotificationStatus.UNREAD))
                .criticalNotifications(notificationRepository.countByPriority(NotificationPriority.CRITICAL))
                .todayNotifications(notificationRepository.countByCreatedAtAfter(today))
                .notificationsByType(notificationRepository.countGroupByNotificationType())
                .notificationsByPriority(notificationRepository.countGroupByPriority())
                .build();
    }

    @Transactional
    public NotificationResponse sendNotification(NotificationEvent event) {
        processorService.processNotification(event, false);
        // Save notification for the first user as example (or extend logic as needed)
        String userId = event.getTargetUserIds().get(0);
        Notification notification = Notification.builder()
                .userId(userId)
                .notificationType(event.getNotificationType())
                .priority(event.getPriority())
                .content(event.getContent())
                .readStatus(NotificationStatus.UNREAD)
                .build();
        Notification saved = notificationRepository.save(notification);
        return convertToResponse(saved);
    }

    private NotificationResponse convertToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .title(notification.getTitle())
                .content(notification.getContent())
                .notificationType(notification.getNotificationType())
                .priority(notification.getPriority())
                .readStatus(notification.getReadStatus())
                .createdAt(notification.getCreatedAt())
                .build();
    }

    public List<String> getNotificationTypes() {
        return notificationRepository.findDistinctNotificationTypes();
    }
}
