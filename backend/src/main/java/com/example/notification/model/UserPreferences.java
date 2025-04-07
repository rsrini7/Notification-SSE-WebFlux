package com.example.notification.model;

import java.util.Set;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_preferences")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferences {
    @Id
    private String userId;
    
    private boolean emailEnabled;
    private boolean webSocketEnabled;
    private NotificationPriority minimumEmailPriority;
    
    @ElementCollection
    private Set<String> mutedNotificationTypes;
}