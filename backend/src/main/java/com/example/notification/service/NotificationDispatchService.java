package com.example.notification.service;

import com.example.notification.dto.NotificationResponse;
import com.example.notification.websocket.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationDispatchService {

    private final WebSocketSessionManager webSocketSessionManager;
    private final EmailService emailService; // Assuming EmailService is correctly defined

    @Value("${notification.websocket.user-notifications-destination}")
    private String userNotificationsDestination;

    public void dispatchToWebSocket(String userId, NotificationResponse response) {
        if (webSocketSessionManager.isUserConnected(userId)) {
            webSocketSessionManager.sendToUser(userId, userNotificationsDestination, response);
            log.debug("Sent notification ID {} to user {} via WebSocket", response.getId(), userId);
        } else {
            log.debug("User {} not connected via WebSocket for notification ID {}", userId, response.getId());
        }
    }

    public void dispatchToEmail(String userId, NotificationResponse response) {
        // Assuming EmailService has a method like sendNotificationEmail(String userId, NotificationResponse notification)
        emailService.sendNotificationEmail(userId, response);
        log.debug("Sent critical notification ID {} to user {} via email", response.getId(), userId);
    }
}