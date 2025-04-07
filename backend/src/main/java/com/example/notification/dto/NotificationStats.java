package com.example.notification.dto;

import lombok.Data;
import lombok.Builder;
import java.util.Map;

@Data
@Builder
public class NotificationStats {
    private long totalNotifications;
    private long unreadNotifications;
    private long criticalNotifications;
    private long todayNotifications;
    private Map<String, Long> notificationsByType;
    private Map<String, Long> notificationsByPriority;
}
