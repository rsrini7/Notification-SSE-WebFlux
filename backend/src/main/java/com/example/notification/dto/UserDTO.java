package com.example.notification.dto;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class UserDTO {
    private String userId;
    private String username;
    private UserPreferencesDTO preferences;
    private long unreadNotificationsCount;
    private boolean isActive;
}