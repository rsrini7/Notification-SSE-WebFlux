package com.example.notification.controller;

import com.example.notification.dto.NotificationEvent;
import com.example.notification.dto.NotificationResponse;
import com.example.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

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

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Get all notifications for a user with pagination
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<NotificationResponse>> getUserNotifications(
            @PathVariable String userId,
            Pageable pageable) {
        return ResponseEntity.ok(notificationService.getUserNotifications(userId, pageable));
    }

    /**
     * Get unread notifications for a user with pagination
     */
    @GetMapping("/user/{userId}/unread")
    public ResponseEntity<Page<NotificationResponse>> getUnreadNotifications(
            @PathVariable String userId,
            Pageable pageable) {
        return ResponseEntity.ok(notificationService.getUnreadNotifications(userId, pageable));
    }

    /**
     * Get notifications by type for a user with pagination
     */
    @GetMapping("/user/{userId}/type/{notificationType}")
    public ResponseEntity<Page<NotificationResponse>> getNotificationsByType(
            @PathVariable String userId,
            @PathVariable String notificationType,
            Pageable pageable) {
        return ResponseEntity.ok(notificationService.getNotificationsByType(userId, notificationType, pageable));
    }

    /**
     * Search notifications by content for a user with pagination
     */
    @GetMapping("/user/{userId}/search")
    public ResponseEntity<Page<NotificationResponse>> searchNotifications(
            @PathVariable String userId,
            @RequestParam String searchTerm,
            Pageable pageable) {
        return ResponseEntity.ok(notificationService.searchNotifications(userId, searchTerm, pageable));
    }

    /**
     * Get a notification by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<NotificationResponse> getNotificationById(
            @PathVariable Long id) {
        return ResponseEntity.ok(notificationService.getNotificationById(id));
    }

    /**
     * Mark a notification as read
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markNotificationAsRead(
            @PathVariable Long id,
            @RequestParam String userId) {
        notificationService.markAsRead(id, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * Mark all notifications as read for a user
     */
    @PutMapping("/user/{userId}/read-all")
    public ResponseEntity<Integer> markAllNotificationsAsRead(
            @PathVariable String userId) {
        return ResponseEntity.ok(notificationService.markAllAsRead(userId));
    }

    /**
     * Count unread notifications for a user
     */
    @GetMapping("/user/{userId}/unread/count")
    public ResponseEntity<Long> countUnreadNotifications(
            @PathVariable String userId) {
        return ResponseEntity.ok(notificationService.countUnreadNotifications(userId));
    }


    /**
     * Send a notification to specific users
     */
    @PostMapping
    public ResponseEntity<Void> sendNotification(@Valid @RequestBody NotificationEvent event) {
        log.info("REST request to send notification: {}", event);
        notificationService.sendNotification(event);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    /**
     * Send a critical notification to specific users
     */
    @PostMapping("/critical")
    public ResponseEntity<Void> sendCriticalNotification(@Valid @RequestBody NotificationEvent event) {
        log.info("REST request to send critical notification: {}", event);
        // Criticality is now determined by priority. Client should set priority to CRITICAL.
        notificationService.sendNotification(event);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @GetMapping("/types")
    public ResponseEntity<List<String>> getNotificationTypes() {
        return ResponseEntity.ok(notificationService.getNotificationTypes());
    }
}
