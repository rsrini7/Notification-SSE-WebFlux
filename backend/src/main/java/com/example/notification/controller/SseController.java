package com.example.notification.controller;

import com.example.notification.model.UserPreferences;
import com.example.notification.repository.UserPreferencesRepository;
import com.example.notification.security.JwtTokenProvider;
import com.example.notification.service.SseEmitterManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.http.MediaType;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.io.IOException;
import java.util.Random;

// SseController.java
@RestController
@RequestMapping("/api/notifications")
public class SseController {

    private static final Logger logger = LoggerFactory.getLogger(SseController.class);
    private final SseEmitterManager sseEmitterManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserPreferencesRepository userPreferencesRepository;

    // Constructor injection
    public SseController(
        SseEmitterManager sseEmitterManager,
        JwtTokenProvider jwtTokenProvider,
        UserPreferencesRepository userPreferencesRepository) {
        this.sseEmitterManager = sseEmitterManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userPreferencesRepository = userPreferencesRepository;
    }

    @GetMapping(value="/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> streamEvents(@RequestParam("token") String token) {
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            logger.warn("SSE connection attempt with invalid or missing token.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String userId = jwtTokenProvider.getUserIdFromJWT(token);
        Optional<UserPreferences> userPreferencesOptional = userPreferencesRepository.findByUserId(userId);
        if (userId == null) {
            logger.warn("Could not extract userId from token for SSE connection.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (userPreferencesOptional.isPresent()) {
            UserPreferences preferences = userPreferencesOptional.get();
            if (!preferences.isSseEnabled()) {
                logger.warn("SSE connection denied for user: {}. SSE is disabled in user preferences.", userId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).<SseEmitter>build();
            }
            // If SSE is enabled, proceed (no specific action here, logic continues below)
            logger.info("SSE connection allowed for user: {}. SSE is enabled in user preferences.", userId);
        } else {
            // If no preferences are found, proceed (default to SSE enabled)
            logger.info("No user preferences found for user: {}. Allowing SSE connection by default.", userId);
        }

        final String userKey = userId; // Effectively final for lambdas

        // Close any existing emitters for this user before creating a new one
        sseEmitterManager.closeAndRemoveEmittersForUser(userKey);

        // Set a timeout
        SseEmitter emitter = new SseEmitter(TimeUnit.HOURS.toMillis(1));

        emitter.onCompletion(() -> {
            logger.info("SseEmitter completed for user: {}", userKey);
            try{
                sseEmitterManager.removeEmitter(userKey, emitter);
            }catch (Exception e){
                logger.debug("Error onCompletion removing SseEmitter for user: {} - Error: {}", userKey, e.getMessage());
            }
            //emitter.complete();
        });

        emitter.onTimeout(() -> {
            logger.info("SseEmitter timed out for user: {}. Completing the emitter.", userKey); // Log message can be updated
            try{
                sseEmitterManager.removeEmitter(userKey, emitter);
            }catch (Exception e){
                logger.debug("Error onTimeout removing SseEmitter for user: {} - Error: {}", userKey, e.getMessage());
            }

            //emitter.complete();
        });

        emitter.onError(e -> {
            if(e instanceof IOException && e.getMessage() != null && (
                e.getMessage().contains("Broken pipe") || 
                e.getMessage().contains("aborted by the software")) ||
                e.getMessage().contains("Connection reset") || 
                e.getMessage().contains("closed")) {
                logger.debug("Client Connection Closed. SseEmitter error for user: {} - Broken pipe or connection reset detected, removing emitter.", userKey,e.getMessage());
            } else {
                logger.error("SseEmitter error for user: {} - Error: {}", userKey, e.getMessage());
            }

            try{
                // Attempt to remove the emitter from the manager
                sseEmitterManager.removeEmitter(userKey, emitter);
            } catch (Exception ex) {
                logger.debug("Error removing SseEmitter for user: {} - Error: {}", userKey, ex.getMessage());
            }
            
            try{
                // Complete the emitter with error to notify the client
                emitter.complete();
            } catch (Exception ex) {
                logger.debug("Error completing SseEmitter for user: {} - Error: {}", userKey, ex.getMessage());
            }
            
        });

        sseEmitterManager.addEmitter(userKey, emitter);

        try {
            // Send an initial event to confirm connection
            String randomId = String.valueOf(new Random().nextInt(3) + 1);
            emitter.send(SseEmitter.event().id(randomId).name("INIT").data("Connection established for user: " + userKey));
            logger.info("SSE connection established and initial events sent for user: {}", userKey);
        } catch (IOException e) { 
             if(e.getMessage() != null && (
                e.getMessage().contains("Broken pipe") || 
                e.getMessage().contains("aborted by the software")) ||
                e.getMessage().contains("Connection reset") || 
                e.getMessage().contains("closed")) {
                logger.debug("Client Connection Closed. SseEmitter error for user: {} - Broken pipe or connection reset detected, removing emitter.", userKey,e.getMessage());
            } else {
                logger.error("Error sending initial INIT event to user: {} - Error: {}", userKey, e.getMessage());
            }
            
            sseEmitterManager.removeEmitter(userKey, emitter);

            try{
                // Complete the emitter with error to notify the client
                emitter.complete();
            } catch (Exception ex) {
                logger.debug("Error completing SseEmitter for user: {} - Error: {}", userKey, ex.getMessage());
            }

            // return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            return ResponseEntity.status(HttpStatus.OK).build(); // Return OK to indicate the connection was established, even if the initial send failed
        }
        catch (Exception e) {
            logger.error("Unexpected error while sending initial event for user: {} - Error: {}", userKey, e.getMessage());
            try {
                // Attempt to remove the emitter from the manager
                sseEmitterManager.removeEmitter(userKey, emitter);
                emitter.complete();
            } catch (Exception ex) {
                logger.debug("Error removing SseEmitter for user: {} - Error: {}", userKey, ex.getMessage());
            }
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        return ResponseEntity.ok(emitter);
    }
}
