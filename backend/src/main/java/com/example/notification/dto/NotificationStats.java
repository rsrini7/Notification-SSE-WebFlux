package com.example.notification.dto;

import lombok.Data;
import lombok.Builder;
import java.util.Map;
import com.example.notification.model.NotificationPriority;

@Data
@Builder
public class NotificationStats {
    private long totalNotifications;
    private long unreadNotifications;
    private long criticalNotifications;
    private long todayNotifications;
    private Map<String, Long> notificationsByType;
    private Map<NotificationPriority, Long> notificationsByPriority;
    private Double readRate;
}
