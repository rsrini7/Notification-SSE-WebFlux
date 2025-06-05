package com.example.notification.dto;

import lombok.Data;
import lombok.Builder;
import java.util.Set;

@Data
@Builder
public class UserPreferencesDTO {
    private boolean emailEnabled;
    private boolean sseEnabled;
    private String minimumEmailPriority;
    private Set<String> mutedNotificationTypes;
}