package com.example.notification.dto;

import com.example.notification.model.NotificationPriority;
import com.example.notification.model.NotificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private Long id;
    private String userId;
    private String title;
    private String content;
    private String notificationType;
    private String sourceService;
    private NotificationPriority priority;
    private NotificationStatus readStatus;
    private LocalDateTime createdAt;
    private Object metadata;
    private Object tags;
}
