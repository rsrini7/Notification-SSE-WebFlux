package com.example.notification.health;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.example.notification.websocket.WebSocketSessionManager;

@Component
public class NotificationHealthIndicator implements HealthIndicator {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final WebSocketSessionManager webSocketManager;

    public NotificationHealthIndicator(KafkaTemplate<String, String> kafkaTemplate,
                                      WebSocketSessionManager webSocketManager) {
        this.kafkaTemplate = kafkaTemplate;
        this.webSocketManager = webSocketManager;
    }
    
    @Override
    public Health health() {
        Map<String, Object> details = new HashMap<>();
        
        // Check Kafka connectivity
        try {
            kafkaTemplate.getDefaultTopic();
            details.put("kafka", "UP");
        } catch (Exception e) {
            details.put("kafka", "DOWN");
            return Health.down().withDetails(details).build();
        }
        
        // Check WebSocket status
        details.put("activeConnections", webSocketManager.getConnectedUserCount());

        return Health.up().withDetails(details).build();
    }
}