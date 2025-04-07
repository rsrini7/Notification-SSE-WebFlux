package com.example.notification.model;

import lombok.Builder;
import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Builder
@Table(name = "notifications")
public class Notification {

    private String sourceService;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String metadata;

    private String tags;
    
    private String userId;
    private String title;
    private String content;
    private String notificationType;
    
    @Enumerated(EnumType.STRING)
    private NotificationPriority priority;
    
    @Enumerated(EnumType.STRING)
    private NotificationStatus readStatus;
    
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (readStatus == null) {
            readStatus = NotificationStatus.UNREAD;
        }
    }
}
