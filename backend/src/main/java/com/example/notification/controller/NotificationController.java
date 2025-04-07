package com.example.notification.controller;

import com.example.notification.dto.NotificationEvent;
import com.example.notification.dto.NotificationResponse;
import com.example.notification.service.NotificationProcessorService;
import com.example.notification.service.NotificationService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for notification operations
 */
@RestController
@RequestMapping("/api/notifications")
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationProcessorService processorService;

    public NotificationController(NotificationService notificationService,
                                 NotificationProcessorService processorService) {
        this.notificationService = notificationService;
        this.processorService = processorService;
    }

    /**
     * Get all notifications for a user with pagination
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<NotificationResponse>> getNotificationsForUser(
            @PathVariable String userId,
            Pageable pageable) {
        log.info("REST request to get notifications for user: {}", userId);
        Page<NotificationResponse> notifications = notificationService.getNotificationsForUser(userId, pageable);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Get unread notifications for a user with pagination
     */
    @GetMapping("/user/{userId}/unread")
    public ResponseEntity<Page<NotificationResponse>> getUnreadNotificationsForUser(
            @PathVariable String userId,
            Pageable pageable) {
        log.info("REST request to get unread notifications for user: {}", userId);
        Page<NotificationResponse> notifications = notificationService.getUnreadNotificationsForUser(userId, pageable);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Get notifications by type for a user with pagination
     */
    @GetMapping("/user/{userId}/type/{notificationType}")
    public ResponseEntity<Page<NotificationResponse>> getNotificationsByType(
            @PathVariable String userId,
            @PathVariable String notificationType,
            Pageable pageable) {
        log.info("REST request to get notifications of type {} for user: {}", notificationType, userId);
        Page<NotificationResponse> notifications = notificationService.getNotificationsByType(userId, notificationType, pageable);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Search notifications by content for a user with pagination
     */
    @GetMapping("/user/{userId}/search")
    public ResponseEntity<Page<NotificationResponse>> searchNotifications(
            @PathVariable String userId,
            @RequestParam String searchTerm,
            Pageable pageable) {
        log.info("REST request to search notifications with term '{}' for user: {}", searchTerm, userId);
        Page<NotificationResponse> notifications = notificationService.searchNotifications(userId, searchTerm, pageable);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Get a notification by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<NotificationResponse> getNotificationById(@PathVariable Long id) {
        log.info("REST request to get notification by ID: {}", id);
        return notificationService.getNotificationById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Mark a notification as read
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markNotificationAsRead(
            @PathVariable Long id,
            @RequestParam String userId) {
        log.info("REST request to mark notification {} as read for user: {}", id, userId);
        boolean success = notificationService.markNotificationAsRead(id, userId);
        return success ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    /**
     * Mark all notifications as read for a user
     */
    @PutMapping("/user/{userId}/read-all")
    public ResponseEntity<Integer> markAllNotificationsAsRead(@PathVariable String userId) {
        log.info("REST request to mark all notifications as read for user: {}", userId);
        int count = notificationService.markAllNotificationsAsRead(userId);
        return ResponseEntity.ok(count);
    }

    /**
     * Count unread notifications for a user
     */
    @GetMapping("/user/{userId}/unread/count")
    public ResponseEntity<Long> countUnreadNotifications(@PathVariable String userId) {
        log.info("REST request to count unread notifications for user: {}", userId);
        long count = notificationService.countUnreadNotifications(userId);
        return ResponseEntity.ok(count);
    }

    /**
     * Send a broadcast notification
     */
    @PostMapping("/broadcast")
    public ResponseEntity<Void> sendBroadcastNotification(@Valid @RequestBody NotificationEvent event) {
        log.info("REST request to send broadcast notification: {}", event);
        notificationService.sendBroadcastNotification(event);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    /**
     * Send a notification to specific users
     */
    @PostMapping
    public ResponseEntity<Void> sendNotification(@Valid @RequestBody NotificationEvent event) {
        log.info("REST request to send notification: {}", event);
        processorService.processNotification(event, false);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    /**
     * Send a critical notification to specific users
     */
    @PostMapping("/critical")
    public ResponseEntity<Void> sendCriticalNotification(@Valid @RequestBody NotificationEvent event) {
        log.info("REST request to send critical notification: {}", event);
        processorService.processNotification(event, true);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
}