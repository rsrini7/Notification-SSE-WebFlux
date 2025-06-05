package com.example.notification.dto;

import com.example.notification.model.NotificationPriority;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO representing a notification event published to Kafka by producer services
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {
    
    // Target user ID(s) - can be a single user or multiple users for broadcasts
    private List<String> targetUserIds;
    
    // Source service that generated this notification
    private String sourceService;
    
    // Type of notification (e.g., "TICKET_CREATED", "ALERT", "SYSTEM_MAINTENANCE")
    private String notificationType;
    
    // Priority level of the notification
    private NotificationPriority priority;
    
    // Main notification message content
    private String content;
    
    // Additional structured data related to the notification
    private Map<String, Object> metadata;
    
    // Optional tags for categorization and filtering
    private List<String> tags;
    // Notification title
    private String title;
    private boolean isCritical;
}