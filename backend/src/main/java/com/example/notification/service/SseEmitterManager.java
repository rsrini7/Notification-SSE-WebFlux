package com.example.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Random;

@Service
public class SseEmitterManager {

    private static final Logger logger = LoggerFactory.getLogger(SseEmitterManager.class);
    private final ConcurrentHashMap<String, Sinks.Many<ServerSentEvent<String>>> userSinks = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public SseEmitterManager(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Flux<ServerSentEvent<String>> addEmitter(String userId) {
        Sinks.Many<ServerSentEvent<String>> sink = Sinks.many().replay().latest();
        userSinks.put(userId, sink);
        logger.info("Added sink for user: {}", userId);
        return sink.asFlux();
    }

    public void removeEmitter(String userId) {
        if (userId == null) {
            logger.warn("Attempted to remove sink with null userId.");
            return;
        }
        Sinks.Many<ServerSentEvent<String>> sink = userSinks.remove(userId);
        if (sink != null) {
            sink.tryEmitComplete();
            logger.info("Removed sink for user: {}", userId);
        } else {
            logger.debug("No sink found for user: {}. Nothing to remove.", userId);
        }
    }

    public void sendToUser(String userId, Object data) {
        Sinks.Many<ServerSentEvent<String>> sink = userSinks.get(userId);
        if (sink != null) {
            String randomId = String.valueOf(new Random().nextInt(3) + 1);
            try {
                String jsonData = objectMapper.writeValueAsString(data);
                ServerSentEvent<String> event = ServerSentEvent.<String>builder()
                        .id(randomId)
                        .event("notification")
                        .data(jsonData)
                        .build();
                Sinks.EmitResult result = sink.tryEmitNext(event);
                if (result.isSuccess()) {
                    logger.info("Sent event to user: {} - Data: {}", userId, data);
                } else {
                    logger.warn("Failed to send event to user: {}. Result: {}", userId, result);
                }
            } catch (Exception e) {
                logger.error("Error serializing data to JSON for user: {}", userId, e);
            }
        } else {
            logger.warn("No sink found for user: {}", userId);
        }
    }

    public void closeAndRemoveEmittersForUser(String userId) {
        removeEmitter(userId);
    }

    @Scheduled(fixedRate = 20000) // Send heartbeat every 20 seconds
    public void sendHeartbeats() {
        userSinks.forEach((userId, sink) -> {
            if (userId == null || sink == null) {
                logger.warn("Skipping heartbeat for null userId or sink.");
                return;
            }
            try {
                String randomId = String.valueOf(new Random().nextInt(3) + 1);
                ServerSentEvent<String> heartbeatEvent = ServerSentEvent.<String>builder()
                        .id(randomId)
                        .event("KEEPALIVE")
                        .data("ping")
                        .build();
                Sinks.EmitResult result = sink.tryEmitNext(heartbeatEvent);
                if (result.isSuccess()) {
                    logger.info("Sent KEEPALIVE event to user: {}", userId);
                } else {
                    logger.warn("Failed to send KEEPALIVE event to user: {}, removing sink. Result: {}", userId, result);
                    removeEmitter(userId);
                }
            } catch (Exception e) {
                logger.error("Error occurred while sending heartbeats for user: {} - Error: {}", userId, e.getMessage(), e);
                removeEmitter(userId);
            }
        });
    }
}
