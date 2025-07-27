package com.example.notification.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.util.Set;
import java.util.HashSet;

@Data
@Entity
@NoArgsConstructor
@Table(name = "user_preferences")
public class UserPreferences {
    @Id
    private String userId;
    
    private boolean emailEnabled = true;
    private boolean sseEnabled = true;
    private String minimumEmailPriority = "NORMAL";
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "muted_notification_types")
    private Set<String> mutedNotificationTypes = new HashSet<>();
}
