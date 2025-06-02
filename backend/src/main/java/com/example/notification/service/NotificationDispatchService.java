package com.example.notification.service;

import com.example.notification.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationDispatchService {

    private final SseEmitterManager sseEmitterManager;
    private final EmailService emailService;

    public void dispatchNotification(String userId, NotificationResponse response) {
        // SseEmitterManager handles whether user is connected or has an emitter
        sseEmitterManager.sendToUser(userId, response);
        log.debug("Attempted to send notification ID {} to user {} via SSE", response.getId(), userId);
        // No need to check if user is connected here, SseEmitterManager handles it.
    }

    public void dispatchToEmail(String userId, NotificationResponse response) {
        // Assuming EmailService has a method like sendNotificationEmail(String userId, NotificationResponse notification)
        emailService.sendNotificationEmail(userId, response);
        log.debug("Sent critical notification ID {} to user {} via email", response.getId(), userId);
    }
}