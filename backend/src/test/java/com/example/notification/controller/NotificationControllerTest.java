package com.example.notification.controller;

import com.example.notification.dto.NotificationEvent;
import com.example.notification.model.NotificationPriority;
import com.example.notification.security.JwtTokenProvider;
import com.example.notification.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals; // Added import
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
public class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @WithMockUser(roles = "USER")
    void sendNotification_sendsCorrectEvent() throws Exception {
        NotificationEvent event = NotificationEvent.builder()
                .sourceService("test-service")
                .notificationType("TEST_NORMAL")
                .title("Test Message")
                .content("This is a test message.")
                .priority(NotificationPriority.HIGH)
                .targetUserIds(Collections.singletonList("user1"))
                .build();

        mockMvc.perform(post("/api/notifications")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isAccepted());

        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(notificationService).sendNotification(eventCaptor.capture());

        assertEquals("test-service", eventCaptor.getValue().getSourceService());
        assertEquals(NotificationPriority.HIGH, eventCaptor.getValue().getPriority());
    }

    @Test
    @WithMockUser(roles = "USER")
    void sendCriticalNotification_sendsCorrectEvent() throws Exception {
        NotificationEvent event = NotificationEvent.builder()
                .sourceService("critical-test-service")
                .notificationType("TEST_CRITICAL")
                .title("Critical Test Message")
                .content("This is a critical test message.")
                .priority(NotificationPriority.CRITICAL)
                .targetUserIds(Collections.singletonList("user1"))
                .build();

        mockMvc.perform(post("/api/notifications/critical")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isAccepted());

        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(notificationService).sendNotification(eventCaptor.capture());

        assertEquals("critical-test-service", eventCaptor.getValue().getSourceService());
        assertEquals(NotificationPriority.CRITICAL, eventCaptor.getValue().getPriority());
    }
}
