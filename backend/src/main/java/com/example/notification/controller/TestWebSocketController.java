package com.example.notification.controller;

import com.example.notification.dto.NotificationEvent;
import com.example.notification.service.NotificationService;
import com.example.notification.websocket.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@Slf4j
@RequiredArgsConstructor
public class TestWebSocketController {

    private final NotificationService notificationService;
    private final WebSocketSessionManager webSocketSessionManager;
    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping("/send-notification")
    public ResponseEntity<Map<String, String>> sendTestNotification(
            @RequestParam String userId,
            @RequestParam(required = false, defaultValue = "Test Title") String title,
            @RequestParam(required = false, defaultValue = "This is a test notification") String content) {
        
        log.info("Sending test notification to user: {}", userId);
        
        // Create a test notification
        NotificationEvent event = new NotificationEvent();
        event.setTargetUserIds(Arrays.asList(userId));
        event.setTitle(title);
        event.setContent(content);
        event.setNotificationType("TEST_NOTIFICATION");
        event.setSourceService("test-service");
        
        // Send the notification
        notificationService.sendNotification(event);
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Test notification sent to user: " + userId);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/check-connection/{userId}")
    public ResponseEntity<Map<String, Object>> checkWebSocketConnection(@PathVariable String userId) {
        Map<String, Object> response = new HashMap<>();
        boolean isConnected = webSocketSessionManager.isUserConnected(userId);
        
        response.put("userId", userId);
        response.put("isConnected", isConnected);
        response.put("timestamp", System.currentTimeMillis());
        
        if (isConnected) {
            response.put("message", "User has an active WebSocket connection");
        } else {
            response.put("message", "No active WebSocket connection found for user");
        }
        
        return ResponseEntity.ok(response);
    }
}
