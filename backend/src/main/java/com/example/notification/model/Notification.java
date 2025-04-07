package com.example.notification.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_user_id", columnList = "userId"),
    @Index(name = "idx_notification_type", columnList = "notificationType"),
    @Index(name = "idx_priority", columnList = "priority"),
    @Index(name = "idx_created_at", columnList = "createdAt"),
    @Index(name = "idx_read_status", columnList = "readStatus")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String userId;
    
    @Column(nullable = false)
    private String sourceService;
    
    @Column(nullable = false)
    private String notificationType;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationPriority priority;
    
    @Column(nullable = false, length = 1000)
    private String content;
    
    @Column(columnDefinition = "JSON")
    private String metadata;
    
    @Column(columnDefinition = "JSON")
    private String tags;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus readStatus;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (readStatus == null) {
            readStatus = NotificationStatus.UNREAD;
        }
    }
}