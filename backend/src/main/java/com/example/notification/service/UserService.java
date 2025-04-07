package com.example.notification.service;

import com.example.notification.dto.UserDTO;
import com.example.notification.dto.UserPreferencesDTO;
import com.example.notification.model.User;
import com.example.notification.model.UserPreferences;
import com.example.notification.repository.UserPreferencesRepository;
import com.example.notification.repository.UserRepository;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for retrieving user information
 * In a real application, this would integrate with a user management service
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;
    private final UserPreferencesRepository preferencesRepository;
    private final NotificationService notificationService;

    /**
     * Get a user's email address
     * In a real application, this would call a user service or database
     * @param userId The user ID
     * @return The user's email address
     */
    public String getUserEmail(String userId) {
        // This is a mock implementation
        // In a real application, this would call a user service or database
        log.info("Getting email for user: {}", userId);
        
        // For demonstration purposes, we'll return a fake email
        // In production, this would integrate with your user management system
        return userId + "@example.com";
    }
    
    /**
     * Check if a user exists
     * @param userId The user ID
     * @return True if the user exists, false otherwise
     */
    public boolean userExists(String userId) {
        // This is a mock implementation
        // In a real application, this would call a user service or database
        log.info("Checking if user exists: {}", userId);
        
        // For demonstration purposes, we'll assume all users exist
        // In production, this would integrate with your user management system
        return true;
    }

    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private UserDTO convertToDTO(User user) {
        UserPreferences prefs = preferencesRepository.findByUserId(user.getUsername())
                .orElseGet(() -> {
                    UserPreferences defaultPrefs = new UserPreferences();
                    defaultPrefs.setUserId(user.getUsername());
                    return defaultPrefs;
                });

        return UserDTO.builder()
                .userId(user.getUsername())
                .username(user.getUsername())
                .preferences(convertToPreferencesDTO(prefs))
                .unreadNotificationsCount(
                    notificationService.countUnreadNotifications(user.getUsername()))
                .isActive(user.isEnabled())
                .build();
    }

    private UserPreferencesDTO convertToPreferencesDTO(UserPreferences preferences) {
        return UserPreferencesDTO.builder()
                .emailEnabled(preferences.isEmailEnabled())
                .websocketEnabled(preferences.isWebsocketEnabled())
                .minimumEmailPriority(preferences.getMinimumEmailPriority())
                .mutedNotificationTypes(preferences.getMutedNotificationTypes())
                .build();
    }
}
