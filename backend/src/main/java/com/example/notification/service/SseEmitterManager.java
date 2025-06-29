package com.example.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
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

        if (userId == null || emitter == null) {
            logger.warn("Attempted to remove SseEmitter with null userId or emitter.");
            return;
        }

        synchronized (userId.intern()) { // Synchronize on userId to prevent concurrent modification issues
            List<SseEmitter> emitters = userEmitters.get(userId);
            if (emitters != null) {
                if(emitters.contains(emitter)) {
                    emitters.remove(emitter);
                    logger.debug("Removed SseEmitter for user: {} - Emitter: {}", userId, emitter);

                    if (emitters.isEmpty()) {
                        userEmitters.remove(userId);
                        logger.debug("No more emitters for user: {}. Removed from userEmitters map.", userId);
                    }
                } else {
                    logger.debug("Attempted to remove SseEmitter for user: {} but emitter was not found in the list.", userId);
                }
            } else{
                logger.debug("No SseEmitters found for user: {}. Nothing to remove.", userId);
            }
        }
        logger.info("Removed SseEmitter for user: {}", userId);
    }

    public void sendToUser(String userId, Object data) {
        List<SseEmitter> emitters = userEmitters.get(userId);
        if (emitters != null && !emitters.isEmpty()) {

            // Create a copy to prevent concurrent modification
            List<SseEmitter> emittersCopy = new ArrayList<>(emitters);
            List<SseEmitter> emittersToRemove = new ArrayList<>();

            for (SseEmitter emitter : emittersCopy) {
                try {
                    emitter.send(SseEmitter.event().data(data));
                    logger.info("Sent event to user: {} - Data: {}", userId, data);
                } catch (IOException e) {
                    if(e instanceof IOException && e.getMessage() != null && (
                        e.getMessage().contains("Broken pipe") || 
                        e.getMessage().contains("aborted by the software")) ||
                        e.getMessage().contains("Connection reset") || 
                        e.getMessage().contains("closed")) {
                        logger.debug("Client Connection Closed. SseEmitter error for user: {} - Broken pipe or connection reset detected, removing emitter.", userId, e.getMessage());
                    } else {
                        logger.error("SseEmitter error for user: {} - Error: {}", userId, e.getMessage());
                    }
                    emittersToRemove.add(emitter); // If sending fails, add to removal list
                } catch (Exception e) {
                    logger.error("Unexpected error while sending event to user: {} - Error: {}", userId, e.getMessage(), e);
                    emittersToRemove.add(emitter); // Also consider removing emitter for other unexpected errors during send
                }
            }

            // Remove emitters that failed to send
            for (SseEmitter emitterToRemove : emittersToRemove) {
                try {
                    removeEmitter(userId, emitterToRemove);
                } catch (Exception e) {
                    logger.warn("Error removing SseEmitter for user: {} - Error: {}", userId, e.getMessage());
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
            if(userId == null || emitters == null) {
                logger.warn("Skipping heartbeat for null userId or emitters list.");
                return;
            }
            try {
                // Create a copy of emitters to avoid concurrent modification issues
                List<SseEmitter> emittersCopy = new ArrayList<>(emitters);
                List<SseEmitter> emittersToRemove = new ArrayList<>();
                for (SseEmitter emitter : emittersCopy) {
                    if(emittersToRemove.contains(emitter)) {
                        continue; // Skip if already marked for removal
                    }

                    try {

                        try{
                            if(emitter.getTimeout() <=0) {
                                logger.debug("SseEmitter for user {} has no timeout set, skipping heartbeat.", userId);
                                emittersToRemove.add(emitter); // Mark for removal if no timeout is set
                                continue; // Skip if emitter has no timeout set
                            }
                        }catch (Exception e) {
                            logger.warn("Error checking timeout for SseEmitter for user {}: {}. Marking for removal.", userId, e.getMessage());
                            emittersToRemove.add(emitter); // Mark for removal if there's an error checking timeout     
                            continue; // Skip if there's an error checking timeout
                        }

                        // Send a named event for heartbeat
                         SseEventBuilder event = SseEmitter.event().name("KEEPALIVE").data("ping");
                         emitter.send(event);
                         logger.info("Sent KEEPALIVE event to user: {}", userId); // Log level changed to INFO

                    } catch (IOException e) {
                        logger.warn("Error sending heartbeat to user: {}, removing emitter. Error: {}", userId, e.getMessage());
                        
                        if(e.getMessage() != null){
                            if(e.getMessage().contains("aborted by the software")){
                                logger.debug("Client Connection Closed. SseEmitter error for user: {} - Aborted by the software, removing emitter.", userId, e.getMessage());
                            }
                            else if(
                                e.getMessage().contains("Broken pipe") || 
                                e.getMessage().contains("Connection reset") || 
                                e.getMessage().contains("closed")) {
                                logger.debug("Client Connection Closed. SseEmitter error for user: {} - Broken pipe or connection reset detected, removing emitter.", userId, e.getMessage());
                            } else {
                                logger.error("SseEmitter error sendeing heartbeat to user: {} - Error: {}", userId, e.getMessage());
                            }
                        }
                        // If sending heartbeat fails, likely the connection is dead, so remove it.
                        emittersToRemove.add(emitter); // Mark for removal if sending fails
                    } catch (IllegalStateException e) {
                        logger.warn("SseEmitter for user: {} is in an illegal state and is already complete, removing emitter. Error: {}", userId, e.getMessage());
                        // If emitter is in an illegal state, remove it
                        emittersToRemove.add(emitter); // Mark for removal if it's in an illegal state
                    } catch (Exception e) {
                        logger.error("Unexpected error while sending heartbeat to user: {}, Error: {}", userId, e.getMessage(), e);
                        // Also consider removing emitter for other unexpected errors during send
                        emittersToRemove.add(emitter); // Mark for removal if there's an unexpected error
                    }
                }

                // Remove emitters that failed to send heartbeat
                if(!emittersToRemove.isEmpty()) {
                    for (SseEmitter emitterToRemove : emittersToRemove) {
                        try {
                            removeEmitter(userId, emitterToRemove);
                        } catch (Exception e) {
                            logger.warn("Error removing SseEmitter for user: {} - Error: {}", userId, e.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Error occurred while sending heartbeats for user: {} - Error: {}", userId, e.getMessage(), e);
            }
        });
    }
}
