package com.example.notification.service;

import com.example.notification.model.NotificationType;
import com.example.notification.repository.NotificationTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationTypeService {

    private final NotificationTypeRepository notificationTypeRepository;

    /**
     * Get all active notification types
     */
    public List<NotificationType> getAllActiveNotificationTypes() {
        return notificationTypeRepository.findByActiveTrue();
    }

    /**
     * Get notification type by code
     */
    public NotificationType getNotificationTypeByCode(String code) {
        return notificationTypeRepository.findByTypeCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Notification type not found with code: " + code));
    }

    /**
     * Create a new notification type
     */
    @Transactional
    public NotificationType createNotificationType(String code, String description) {
        if (notificationTypeRepository.findByTypeCode(code).isPresent()) {
            throw new IllegalArgumentException("Notification type with code " + code + " already exists");
        }

        NotificationType type = new NotificationType();
        type.setTypeCode(code);
        type.setDescription(description);
        type.setActive(true);
        
        return notificationTypeRepository.save(type);
    }

    /**
     * Update an existing notification type
     */
    @Transactional
    public NotificationType updateNotificationType(String code, String description, Boolean active) {
        NotificationType type = getNotificationTypeByCode(code);
        
        if (description != null) {
            type.setDescription(description);
        }
        
        if (active != null) {
            type.setActive(active);
        }
        
        return notificationTypeRepository.save(type);
    }

    /**
     * Delete a notification type
     */
    @Transactional
    public void deleteNotificationType(String code) {
        NotificationType type = getNotificationTypeByCode(code);
        notificationTypeRepository.delete(type);
    }
}
