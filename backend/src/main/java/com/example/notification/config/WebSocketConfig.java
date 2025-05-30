package com.example.notification.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
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
        config.enableSimpleBroker("/topic", "/queue")
              .setHeartbeatValue(new long[]{10000, 10000}) // Server expects client heartbeat roughly every 10s, server will send heartbeat roughly every 10s
              .setTaskScheduler(heartBeatTaskScheduler());
        
        // Prefix for messages from clients to server
        config.setApplicationDestinationPrefixes(applicationDestinationPrefix);
        
        // Prefix for user-specific destinations
        config.setUserDestinationPrefix(userDestinationPrefix);
    }

    @Bean
    public TaskScheduler heartBeatTaskScheduler() {
        ThreadPoolTaskScheduler ts = new ThreadPoolTaskScheduler();
        ts.setPoolSize(1);
        ts.setThreadNamePrefix("wss-heartbeat-scheduler-");
        ts.initialize();
        return ts;
    }

    @Bean
    public TaskScheduler sockJsTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2); // Pool size for SockJS tasks
        scheduler.setThreadNamePrefix("sockjs-scheduler-");
        scheduler.initialize();
        return scheduler;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register the WebSocket endpoint that clients use to connect
        registry.addEndpoint(endpoint)
                .setAllowedOriginPatterns("http://localhost:3000", "http://localhost:3001") // Allow specific origins
                .withSockJS()
                .setTaskScheduler(sockJsTaskScheduler()) // Use the new scheduler for SockJS
                .setHeartbeatTime(25000); // SockJS level heartbeats (server send interval)
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authChannelInterceptor);
    }
}