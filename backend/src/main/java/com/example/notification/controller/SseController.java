package com.example.notification.controller;

import com.example.notification.security.JwtTokenProvider;
import com.example.notification.service.SseEmitterManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/notifications")
public class SseController {

    private static final Logger logger = LoggerFactory.getLogger(SseController.class);
    private final SseEmitterManager sseEmitterManager;
    private final JwtTokenProvider jwtTokenProvider;

    // Constructor injection
    public SseController(SseEmitterManager sseEmitterManager, JwtTokenProvider jwtTokenProvider) {
        this.sseEmitterManager = sseEmitterManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @GetMapping("/events")
    public ResponseEntity<SseEmitter> streamEvents(@RequestParam("token") String token) {
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            logger.warn("SSE connection attempt with invalid or missing token.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String userId = jwtTokenProvider.getUserIdFromJWT(token);
        if (userId == null) {
            logger.warn("Could not extract userId from token for SSE connection.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Close and remove any existing emitters for this user before creating a new one
        // sseEmitterManager.closeAndRemoveEmittersForUser(userId);

        // Set a timeout, e.g., 1 hour. Adjust as needed.
        SseEmitter emitter = new SseEmitter(TimeUnit.HOURS.toMillis(1));

        final String userKey = userId; // Effectively final variable for lambda

        emitter.onCompletion(() -> {
            logger.info("SseEmitter completed for user: {}", userKey);
            sseEmitterManager.removeEmitter(userKey, emitter);
        });
        emitter.onTimeout(() -> {
            logger.info("SseEmitter timed out for user: {}", userKey);
            sseEmitterManager.removeEmitter(userKey, emitter);
        });
        emitter.onError(e -> {
            logger.error("SseEmitter error for user: {} - Error: {}", userKey, e.getMessage());
            sseEmitterManager.removeEmitter(userKey, emitter);
        });

        sseEmitterManager.addEmitter(userKey, emitter);

        try {
            // Send an initial event to confirm connection
            emitter.send(SseEmitter.event().name("INIT").data("Connection established"));
            logger.info("SSE connection established for user: {}", userKey);
        } catch (Exception e) {
            logger.error("Error sending initial event to user: {} - Error: {}", userKey, e.getMessage());
            sseEmitterManager.removeEmitter(userKey, emitter); // Clean up on error
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        return ResponseEntity.ok(emitter);
    }
}
