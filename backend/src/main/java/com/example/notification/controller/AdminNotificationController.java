package com.example.notification.controller;

import org.springframework.web.bind.annotation.*;

import com.example.notification.dto.NotificationEvent;
import com.example.notification.dto.NotificationResponse;
import com.example.notification.dto.NotificationStats;
import com.example.notification.service.NotificationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import org.springframework.http.HttpStatus; // Added import
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/admin/notifications")
@Slf4j
@RequiredArgsConstructor
public class AdminNotificationController {

    private final NotificationService notificationService;

    @GetMapping("/stats")
    public ResponseEntity<NotificationStats> getNotificationStats() {
        return ResponseEntity.ok(notificationService.getNotificationStats());
    }

    @GetMapping("/recent")
    public ResponseEntity<List<NotificationResponse>> getRecentNotifications(
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(notificationService.getRecentNotifications(limit));
    }

    @PostMapping("/send")
    public ResponseEntity<String> sendNotification(
            @Valid @RequestBody NotificationEvent event) {
        notificationService.sendNotification(event); // now void
        // Log the action
        log.info("AdminController: sendNotification request processed for event title: {} with critical status: {}", event.getTitle(), event.isCritical());
        return ResponseEntity.accepted().body("Notification request processed for " + event.getTargetUserIds().size() + " user(s). Critical: " + event.isCritical());
    }

    @PostMapping("/broadcast")
    public ResponseEntity<String> sendBroadcastNotification( // Return type changed
            @Valid @RequestBody NotificationEvent event) {
        try {
            log.info("AdminController: Attempting to send broadcast for event title: {}", event.getTitle());
            notificationService.sendBroadcastNotification(event); // Service method is void
            log.info("AdminController: Broadcast processed by service for event title: {}", event.getTitle());
            return ResponseEntity.ok("Broadcast command processed for title: " + event.getTitle());
        } catch (Exception e) {
            log.error("AdminController: Error sending broadcast notification for event title: {}", event.getTitle(), e);
            // Ensure HttpStatus is imported: import org.springframework.http.HttpStatus;
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error sending broadcast: " + e.getMessage());
        }
    }

    @GetMapping("/types")
    public ResponseEntity<List<String>> getNotificationTypes() {
        return ResponseEntity.ok(notificationService.getNotificationTypes());
    }
}