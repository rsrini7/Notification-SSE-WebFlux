package com.example.notification.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${notification.websocket.endpoint}")
    private String endpoint;
    
    @Value("${notification.websocket.user-destination-prefix}")
    private String userDestinationPrefix;
    
    @Value("${notification.websocket.application-destination-prefix}")
    private String applicationDestinationPrefix;

    @Autowired
    private AuthChannelInterceptor authChannelInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable a simple in-memory message broker for sending messages to clients
        // Clients subscribe to these destinations to receive messages
        config.enableSimpleBroker("/topic", "/queue");
        
        // Prefix for messages from clients to server
        config.setApplicationDestinationPrefixes(applicationDestinationPrefix);
        
        // Prefix for user-specific destinations
        config.setUserDestinationPrefix(userDestinationPrefix);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register the WebSocket endpoint that clients use to connect
        registry.addEndpoint(endpoint)
                .setAllowedOriginPatterns("http://localhost:3000", "http://localhost:3001") // Allow specific origins
                .withSockJS(); // Fallback options for browsers that don't support WebSocket
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authChannelInterceptor);
    }
}