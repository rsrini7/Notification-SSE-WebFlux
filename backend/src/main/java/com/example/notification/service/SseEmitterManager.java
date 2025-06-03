package com.example.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled; // Added import
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter.SseEventBuilder; // Added import
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter.SseEventBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SseEmitterManager {

    private static final Logger logger = LoggerFactory.getLogger(SseEmitterManager.class);
    private final ConcurrentHashMap<String, List<SseEmitter>> userEmitters = new ConcurrentHashMap<>();

    public void addEmitter(String userId, SseEmitter emitter) {
        userEmitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        logger.info("Added SseEmitter for user: {}", userId);
    }

    public void removeEmitter(String userId, SseEmitter emitter) {
        List<SseEmitter> emitters = userEmitters.get(userId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                userEmitters.remove(userId);
            }
        }
        logger.info("Removed SseEmitter for user: {}", userId);
    }

    public void sendToUser(String userId, Object data) {
        List<SseEmitter> emitters = userEmitters.get(userId);
        if (emitters != null && !emitters.isEmpty()) {
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event().data(data));
                    logger.info("Sent event to user: {} - Data: {}", userId, data);
                } catch (IOException e) {
                    logger.error("Error sending event to user: {} - Data: {} - Error: {}", userId, data, e.getMessage());
                    // Optionally remove emitter on error
                    // removeEmitter(userId, emitter);
                }
            }
        } else {
            logger.warn("No SseEmitters found for user: {}", userId);
        }
    }

    public void closeAndRemoveEmittersForUser(String userId) {
        List<SseEmitter> existingEmitters = userEmitters.get(userId);
        if (existingEmitters != null && !existingEmitters.isEmpty()) {
            logger.info("Found {} existing SseEmitter(s) for user {}. Completing and removing them before establishing new connection.", existingEmitters.size(), userId);
            // Iterate over a copy for safe removal
            List<SseEmitter> emittersToRemove = new ArrayList<>(existingEmitters);
            for (SseEmitter oldEmitter : emittersToRemove) {
                try {
                    oldEmitter.complete(); // This should trigger its onCompletion callback
                } catch (Exception e) {
                    // Log error and manually remove if completion fails to trigger removeEmitter via callback
                    logger.warn("Error while explicitly completing an old emitter for user {}: {}. Manually removing.", userId, e.getMessage());
                    removeEmitter(userId, oldEmitter); // Ensure it's removed from manager's list
                }
            }
        }
    }

    @Scheduled(fixedRate = 20000) // Send heartbeat every 20 seconds
    public void sendHeartbeats() {
        userEmitters.forEach((userId, emitters) -> {
            if (emitters != null && !emitters.isEmpty()) {
                for (SseEmitter emitter : emitters) {
                    try {
                        // Send a named event for heartbeat
                         SseEventBuilder event = SseEmitter.event().name("KEEPALIVE").data("ping");
                         emitter.send(event);
                         logger.info("Sent KEEPALIVE event to user: {}", userId); // Log level changed to INFO

                    } catch (IOException e) {
                        logger.warn("Error sending heartbeat to user: {}, removing emitter. Error: {}", userId, e.getMessage());
                        // If sending heartbeat fails, likely the connection is dead, so remove it.
                        removeEmitter(userId, emitter);
                    } catch (Exception e) {
                        logger.error("Unexpected error while sending heartbeat to user: {}, Error: {}", userId, e.getMessage(), e);
                        // Also consider removing emitter for other unexpected errors during send
                        removeEmitter(userId, emitter);
                    }
                }
            }
        });
    }
}
