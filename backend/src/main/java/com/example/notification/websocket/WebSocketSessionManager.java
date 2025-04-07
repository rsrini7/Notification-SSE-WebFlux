package com.example.notification.websocket;

import java.security.Principal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import lombok.extern.slf4j.Slf4j;

/**
 * Manages WebSocket sessions and provides methods to send messages to specific users
 */
@Component
@Slf4j
public class WebSocketSessionManager {

    private final SimpMessagingTemplate messagingTemplate;
    
    // In a production environment with multiple instances, this would be stored in Redis/Hazelcast
    private final Map<String, String> userSessionMap = new ConcurrentHashMap<>();

    public WebSocketSessionManager(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Handle WebSocket connect events
     */
    public void handleSessionConnected(SessionConnectEvent event) {
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(event.getMessage());
        String sessionId = headers.getSessionId();
        Principal user = headers.getUser();
        
        if (user != null) {
            String userId = user.getName();
            userSessionMap.put(userId, sessionId);
            log.info("User connected: {} with session ID: {}", userId, sessionId);
        }
    }

    /**
     * Handle WebSocket disconnect events
     */
    public void handleSessionDisconnected(SessionDisconnectEvent event) {
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(event.getMessage());
        Principal user = headers.getUser();
        
        if (user != null) {
            String userId = user.getName();
            userSessionMap.remove(userId);
            log.info("User disconnected: {}", userId);
        }
    }

    /**
     * Send a message to a specific user
     */
    public void sendToUser(String userId, String destination, Object payload) {
        if (isUserConnected(userId)) {
            log.debug("Sending message to user: {} at destination: {}", userId, destination);
            messagingTemplate.convertAndSendToUser(userId, destination, payload);
        } else {
            log.debug("User not connected: {}", userId);
        }
    }

    /**
     * Send a broadcast message to all connected users
     */
    public void sendBroadcast(String destination, Object payload) {
        log.debug("Broadcasting message to all users at destination: {}", destination);
        messagingTemplate.convertAndSend(destination, payload);
    }

    /**
     * Check if a user is currently connected
     */
    public boolean isUserConnected(String userId) {
        return userSessionMap.containsKey(userId);
    }

    /**
     * Get the number of connected users
     */
    public int getConnectedUserCount() {
        return userSessionMap.size();
    }
}