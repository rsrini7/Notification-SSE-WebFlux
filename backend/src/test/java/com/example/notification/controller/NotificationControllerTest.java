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

    @MockBean // Added because NotificationController depends on it
    private com.example.notification.service.NotificationProcessingOrchestrator notificationProcessingOrchestrator;

    // Required for security context to load for @WebMvcTest
    @MockBean
    private JwtTokenProvider jwtTokenProvider;


    @Test
    @WithMockUser(roles = "ADMIN") // Assuming ADMIN role is needed for broadcast
    void sendBroadcastNotification_whenCritical_passesCriticalEventToService() throws Exception {
        NotificationEvent event = NotificationEvent.builder()
                .sourceService("test-critical-service")
                .notificationType("CRITICAL_BROADCAST")
                .title("Critical System Wide Alert")
                .content("This is a test critical broadcast.")
                .priority(NotificationPriority.HIGH)
                .isCritical(true)
                .targetUserIds(Collections.singletonList("broadcast")) // Placeholder, actual users handled by service
                .build();

        mockMvc.perform(post("/api/notifications/broadcast")
                .with(csrf()) // CSRF is disabled globally, but good practice in tests if unsure
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isAccepted()); // Expecting 202 Accepted as per AdminNotificationController example

        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(notificationService).sendBroadcastNotification(captor.capture());
        assertTrue(captor.getValue().isCritical());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void sendBroadcastNotification_whenNotCritical_passesNotCriticalEventToService() throws Exception {
        NotificationEvent event = NotificationEvent.builder()
                .sourceService("test-normal-service")
                .notificationType("NORMAL_BROADCAST")
                .title("Normal System Wide Update")
                .content("This is a test normal broadcast.")
                .priority(NotificationPriority.MEDIUM)
                .isCritical(false)
                .targetUserIds(Collections.singletonList("broadcast"))
                .build();

        mockMvc.perform(post("/api/notifications/broadcast")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isAccepted());

        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(notificationService).sendBroadcastNotification(captor.capture());
        assertFalse(captor.getValue().isCritical());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void sendBroadcastNotification_whenCriticalAbsent_passesNotCriticalEventToService() throws Exception {
        // Manually create JSON to ensure isCritical is absent, relying on default boolean value (false)
        String eventJson = "{"
                + "\"sourceService\":\"test-default-service\","
                + "\"notificationType\":\"DEFAULT_BROADCAST\","
                + "\"title\":\"Default Broadcast\","
                + "\"content\":\"This is a test default broadcast.\","
                + "\"priority\":\"LOW\","
                + "\"targetUserIds\":[\"broadcast\"]"
                // isCritical is absent
                + "}";

        mockMvc.perform(post("/api/notifications/broadcast")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(eventJson))
                .andExpect(status().isAccepted());

        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(notificationService).sendBroadcastNotification(captor.capture());
        assertFalse(captor.getValue().isCritical(), "isCritical should default to false");
    }
}
