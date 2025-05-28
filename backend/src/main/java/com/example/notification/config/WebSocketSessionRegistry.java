package com.example.notification.config;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry to track active WebSocket sessions and their associated authentication.
 */
@Component
public class WebSocketSessionRegistry {
    private final Map<String, Authentication> sessionAuthMap = new ConcurrentHashMap<>();
    private final Map<String, String> userSessionMap = new ConcurrentHashMap<>();

    /**
     * Register a new WebSocket session with its authentication.
     */
    public void registerSession(String sessionId, Authentication authentication) {
        if (sessionId == null || authentication == null) {
            throw new IllegalArgumentException("Session ID and Authentication must not be null");
        }
        
        String userId = authentication.getName();
        // Remove any existing session for this user
        String existingSessionId = userSessionMap.get(userId);
        if (existingSessionId != null) {
            sessionAuthMap.remove(existingSessionId);
        }
        
        sessionAuthMap.put(sessionId, authentication);
        userSessionMap.put(userId, sessionId);
    }

    /**
     * Remove a WebSocket session from the registry.
     */
    public void removeSession(String sessionId) {
        if (sessionId == null) {
            return;
        }
        
        Authentication auth = sessionAuthMap.get(sessionId);
        if (auth != null) {
            userSessionMap.remove(auth.getName());
        }
        sessionAuthMap.remove(sessionId);
    }

    /**
     * Get authentication for a session.
     */
    public Authentication getAuthentication(String sessionId) {
        return sessionId != null ? sessionAuthMap.get(sessionId) : null;
    }

    /**
     * Get the session ID for a user.
     */
    public String getSessionId(String userId) {
        return userId != null ? userSessionMap.get(userId) : null;
    }

    /**
     * Check if a user has an active session.
     */
    public boolean hasActiveSession(String userId) {
        return userId != null && userSessionMap.containsKey(userId);
    }

    /**
     * Get the number of active sessions.
     */
    public int getActiveSessionCount() {
        return sessionAuthMap.size();
    }
}
