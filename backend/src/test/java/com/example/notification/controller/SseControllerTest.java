package com.example.notification.controller;

import com.example.notification.model.UserPreferences;
import com.example.notification.repository.UserPreferencesRepository;
import com.example.notification.security.JwtTokenProvider;
import com.example.notification.service.SseEmitterManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SseControllerTest {

    @Mock
    private SseEmitterManager sseEmitterManager;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserPreferencesRepository userPreferencesRepository;

    @InjectMocks
    private SseController sseController;

    private final String testUserId = "testUser";
    private final String testToken = "test-token";

    @Test
    void whenSseEnabled_thenConnectionAllowed() {
        when(jwtTokenProvider.validateToken(anyString())).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromJWT(anyString())).thenReturn(testUserId);
        UserPreferences preferences = new UserPreferences();
        preferences.setUserId(testUserId);
        preferences.setSseEnabled(true);
        when(userPreferencesRepository.findByUserId(testUserId)).thenReturn(Optional.of(preferences));
        when(sseEmitterManager.addEmitter(testUserId)).thenReturn(Flux.just(ServerSentEvent.<String>builder().data("test").build()));

        Flux<ServerSentEvent<String>> response = sseController.streamEvents("Bearer " + testToken);

        StepVerifier.create(response)
            .expectNextCount(1)
            .verifyComplete();
    }

    @Test
    void whenNoPreferences_thenConnectionAllowedByDefault() {
        when(jwtTokenProvider.validateToken(anyString())).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromJWT(anyString())).thenReturn(testUserId);
        when(userPreferencesRepository.findByUserId(testUserId)).thenReturn(Optional.empty());
        when(sseEmitterManager.addEmitter(testUserId)).thenReturn(Flux.just(ServerSentEvent.<String>builder().data("test").build()));

        Flux<ServerSentEvent<String>> response = sseController.streamEvents("Bearer " + testToken);

        StepVerifier.create(response)
            .expectNextCount(1)
            .verifyComplete();
    }

    @Test
    void whenSseDisabled_thenConnectionForbidden() {
        when(jwtTokenProvider.validateToken(anyString())).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromJWT(anyString())).thenReturn(testUserId);
        UserPreferences preferences = new UserPreferences();
        preferences.setUserId(testUserId);
        preferences.setSseEnabled(false);
        when(userPreferencesRepository.findByUserId(testUserId)).thenReturn(Optional.of(preferences));

        Flux<ServerSentEvent<String>> response = sseController.streamEvents("Bearer " + testToken);

        StepVerifier.create(response)
            .verifyComplete();
    }

    @Test
    void whenInvalidToken_thenUnauthorized() {
        when(jwtTokenProvider.validateToken(anyString())).thenReturn(false);

        Flux<ServerSentEvent<String>> response = sseController.streamEvents("Bearer invalid-token");

        StepVerifier.create(response)
            .verifyComplete();
    }
}
