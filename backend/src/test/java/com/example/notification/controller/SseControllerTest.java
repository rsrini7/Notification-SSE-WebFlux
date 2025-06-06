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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
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

        ResponseEntity<SseEmitter> response = sseController.streamEvents(testToken);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(sseEmitterManager).addEmitter(testUserId, any(SseEmitter.class));
    }

    @Test
    void whenNoPreferences_thenConnectionAllowedByDefault() {
        when(jwtTokenProvider.validateToken(anyString())).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromJWT(anyString())).thenReturn(testUserId);
        when(userPreferencesRepository.findByUserId(testUserId)).thenReturn(Optional.empty());

        ResponseEntity<SseEmitter> response = sseController.streamEvents(testToken);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(sseEmitterManager).addEmitter(testUserId, any(SseEmitter.class));
    }

    @Test
    void whenSseDisabled_thenConnectionForbidden() {
        when(jwtTokenProvider.validateToken(anyString())).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromJWT(anyString())).thenReturn(testUserId);

        UserPreferences preferences = new UserPreferences();
        preferences.setUserId(testUserId);
        preferences.setSseEnabled(false);
        when(userPreferencesRepository.findByUserId(testUserId)).thenReturn(Optional.of(preferences));

        ResponseEntity<SseEmitter> response = sseController.streamEvents(testToken);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void whenInvalidToken_thenUnauthorized() {
        when(jwtTokenProvider.validateToken(anyString())).thenReturn(false);

        ResponseEntity<SseEmitter> response = sseController.streamEvents("invalid-token");

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
}
