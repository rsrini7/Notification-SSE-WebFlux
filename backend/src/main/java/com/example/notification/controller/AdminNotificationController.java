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
    public ResponseEntity<NotificationResponse> sendNotification(
            @Valid @RequestBody NotificationEvent event) {
        return ResponseEntity.ok(notificationService.sendNotification(event));
    }

    @PostMapping("/broadcast")
    public ResponseEntity<NotificationResponse> sendBroadcastNotification(
            @Valid @RequestBody NotificationEvent event) {
        return ResponseEntity.ok(notificationService.sendBroadcastNotification(event));
    }
}