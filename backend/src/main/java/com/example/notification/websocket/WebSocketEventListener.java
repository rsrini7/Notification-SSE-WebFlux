package com.example.notification.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * Listens for WebSocket connection and disconnection events
 * and delegates handling to the WebSocketSessionManager
 */
@Component
@Slf4j
public class WebSocketEventListener {

    private final WebSocketSessionManager sessionManager;

    public WebSocketEventListener(WebSocketSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        log.info("Received a new WebSocket connection");
        sessionManager.handleSessionConnected(event);
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        log.info("WebSocket connection disconnected");
        sessionManager.handleSessionDisconnected(event);
    }
}