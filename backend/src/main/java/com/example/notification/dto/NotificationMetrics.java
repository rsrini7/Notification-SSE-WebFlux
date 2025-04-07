package com.example.notification.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class NotificationMetrics {
    private long totalNotifications;
    private long unreadNotifications;
    private long criticalNotifications;
    private long todayNotifications;
    private Map<String, Long> notificationsByType;
    private Map<String, Long> notificationsByPriority;
}
