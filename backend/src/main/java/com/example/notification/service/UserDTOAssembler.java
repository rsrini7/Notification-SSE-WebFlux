package com.example.notification.service;

import com.example.notification.dto.UserDTO;
import com.example.notification.dto.UserPreferencesDTO;
import com.example.notification.model.User;
import com.example.notification.model.UserPreferences;
import com.example.notification.repository.UserPreferencesRepository;
import org.springframework.stereotype.Component;

@Component
public class UserDTOAssembler {
    private final UserPreferencesRepository preferencesRepository;
    private final NotificationService notificationService;

    public UserDTOAssembler(UserPreferencesRepository preferencesRepository, NotificationService notificationService) {
        this.preferencesRepository = preferencesRepository;
        this.notificationService = notificationService;
    }

    public UserDTO toDTO(User user) {
        UserPreferences prefs = preferencesRepository.findByUserId(user.getUsername())
                .orElseGet(() -> {
                    UserPreferences defaultPrefs = new UserPreferences();
                    defaultPrefs.setUserId(user.getUsername());
                    return defaultPrefs;
                });

        return UserDTO.builder()
                .userId(user.getUsername())
                .username(user.getUsername())
                .preferences(toPreferencesDTO(prefs))
                .unreadNotificationsCount(
                    notificationService.countUnreadNotifications(user.getUsername()))
                .isActive(user.isEnabled())
                .build();
    }

    private UserPreferencesDTO toPreferencesDTO(UserPreferences preferences) {
        return UserPreferencesDTO.builder()
                .emailEnabled(preferences.isEmailEnabled())
                .sseEnabled(preferences.isSseEnabled())
                .minimumEmailPriority(preferences.getMinimumEmailPriority())
                .mutedNotificationTypes(preferences.getMutedNotificationTypes())
                .build();
    }
}