package com.example.notification.dto;

import com.example.notification.model.NotificationPriority;
import com.example.notification.model.NotificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;

/**
 * DTO for sending notification data to clients via REST API or WebSocket
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private Long id;
    private String userId;
    private String sourceService;
    private String notificationType;
    private NotificationPriority priority;
    private String content;
    private Map<String, Object> metadata;
    private List<String> tags;
    private LocalDateTime createdAt;
    private NotificationStatus readStatus;
}