package com.example.notification.controller;

import com.example.notification.model.UserPreferences;
import com.example.notification.repository.UserPreferencesRepository;
import com.example.notification.security.JwtTokenProvider;
import com.example.notification.service.SseEmitterManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.MediaType;
import reactor.core.publisher.Flux;
import java.util.Optional;

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
    public Flux<ServerSentEvent<String>> streamEvents(@RequestParam("token") String token) {
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            logger.warn("SSE connection attempt with invalid or missing token.");
            return Flux.empty();
        }

        String userId = jwtTokenProvider.getUserIdFromJWT(token);
        if (userId == null) {
            logger.warn("Could not extract userId from token for SSE connection.");
            return Flux.empty();
        }
        
        Optional<UserPreferences> userPreferencesOptional = userPreferencesRepository.findByUserId(userId);
        if (userPreferencesOptional.isPresent()) {
            UserPreferences preferences = userPreferencesOptional.get();
            if (!preferences.isSseEnabled()) {
                logger.warn("SSE connection denied for user: {}. SSE is disabled in user preferences.", userId);
                return Flux.empty();
            }
            logger.info("SSE connection allowed for user: {}. SSE is enabled in user preferences.", userId);
        } else {
            logger.info("No user preferences found for user: {}. Allowing SSE connection by default.", userId);
        }

        final String userKey = userId;

        sseEmitterManager.closeAndRemoveEmittersForUser(userKey);

        return sseEmitterManager.addEmitter(userKey)
            .doOnSubscribe(subscription -> {
                logger.info("SSE connection subscribed for user: {}", userKey);
                sseEmitterManager.sendToUser(userKey, "Connection established for user: " + userKey);
                logger.info("SSE connection established and initial events sent for user: {}", userKey);
            })
            .doOnCancel(() -> {
                logger.info("Client disconnected, removing emitter for user: {}", userKey);
                sseEmitterManager.removeEmitter(userKey);
            })
            .doOnError(e -> {
                logger.error("Error in SSE stream for user: {}", userKey, e);
                sseEmitterManager.sendToUser(userKey, "Error in SSE stream: " + e.getMessage());
                sseEmitterManager.removeEmitter(userKey);
            })
            .doOnTerminate(() -> logger.info("SSE stream terminated for user: {}", userKey));
    }
}
