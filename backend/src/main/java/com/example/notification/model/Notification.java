package com.example.notification.model;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private String userId;
    
    @Column(name = "source_service")
    private String sourceService;
    
    @ManyToOne
    @JoinColumn(name = "notification_type_id")
    private NotificationType notificationType;
    
    private String title;
    private String content;
    
    @Enumerated(EnumType.STRING)
    private NotificationPriority priority;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "read_status")
    private NotificationStatus readStatus;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    private LocalDateTime emailDispatchedAt;
    
    @Lob
    @Column(columnDefinition = "TEXT")
    private String metadata;

    @Column(columnDefinition = "TEXT")
    private String tags;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (readStatus == null) {
            readStatus = NotificationStatus.UNREAD;
        }
    }
}
