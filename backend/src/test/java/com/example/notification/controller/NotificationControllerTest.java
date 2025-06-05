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

    @MockBean // Added because NotificationController depends on it
    private com.example.notification.service.NotificationProcessingOrchestrator notificationProcessingOrchestrator;

    // Required for security context to load for @WebMvcTest
    @MockBean
    private JwtTokenProvider jwtTokenProvider;


    @Test
    @WithMockUser(roles = "ADMIN")
    void sendBroadcastNotification_whenPriorityCritical_passesPriorityToService() throws Exception {
        NotificationEvent event = NotificationEvent.builder()
                .sourceService("test-critical-service")
                .notificationType("CRITICAL_BROADCAST")
                .title("Critical System Wide Alert")
                .content("This is a test critical broadcast.")
                .priority(NotificationPriority.CRITICAL) // Set to CRITICAL
                // isCritical field removed
                .targetUserIds(Collections.singletonList("broadcast"))
                .build();

        mockMvc.perform(post("/api/notifications/broadcast")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isAccepted());

        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(notificationService).sendBroadcastNotification(captor.capture());
        assertEquals(NotificationPriority.CRITICAL, captor.getValue().getPriority());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void sendBroadcastNotification_whenPriorityNotCritical_passesPriorityToService() throws Exception {
        NotificationEvent event = NotificationEvent.builder()
                .sourceService("test-normal-service")
                .notificationType("NORMAL_BROADCAST")
                .title("Normal System Wide Update")
                .content("This is a test normal broadcast.")
                .priority(NotificationPriority.MEDIUM) // Set to non-CRITICAL
                // isCritical field removed
                .targetUserIds(Collections.singletonList("broadcast"))
                .build();

        mockMvc.perform(post("/api/notifications/broadcast")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isAccepted());

        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(notificationService).sendBroadcastNotification(captor.capture());
        assertEquals(NotificationPriority.MEDIUM, captor.getValue().getPriority());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void sendBroadcastNotification_whenPriorityAbsent_passesPriorityAsNullToService() throws Exception {
        // Manually create JSON to ensure priority is absent
        String eventJson = "{"
                + "\"sourceService\":\"test-default-service\","
                + "\"notificationType\":\"DEFAULT_BROADCAST\","
                + "\"title\":\"Default Broadcast\","
                + "\"content\":\"This is a test default broadcast.\","
                // priority is absent
                + "\"targetUserIds\":[\"broadcast\"]"
                + "}";

        mockMvc.perform(post("/api/notifications/broadcast")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(eventJson))
                .andExpect(status().isAccepted());

        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(notificationService).sendBroadcastNotification(captor.capture());
        assertEquals(null, captor.getValue().getPriority(), "Priority should be null when absent in JSON");
    }

    // Tests for sendNotification and sendCriticalNotification endpoints
    @Test
    @WithMockUser(roles = "USER") // Assuming USER role for direct notifications
    void sendNotification_whenPriorityCritical_callsOrchestratorAsCritical() throws Exception {
        NotificationEvent event = NotificationEvent.builder()
                .sourceService("direct-critical-service")
                .notificationType("DIRECT_CRITICAL")
                .title("Direct Critical Alert")
                .content("This is a direct critical message.")
                .priority(NotificationPriority.CRITICAL)
                .targetUserIds(Collections.singletonList("user1"))
                .build();

        mockMvc.perform(post("/api/notifications")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isAccepted());

        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        ArgumentCaptor<Boolean> criticalFlagCaptor = ArgumentCaptor.forClass(Boolean.class);
        verify(notificationProcessingOrchestrator).processNotification(eventCaptor.capture(), criticalFlagCaptor.capture());

        assertEquals(NotificationPriority.CRITICAL, eventCaptor.getValue().getPriority());
        assertTrue(criticalFlagCaptor.getValue());
    }

    @Test
    @WithMockUser(roles = "USER")
    void sendNotification_whenPriorityNotCritical_callsOrchestratorAsNotCritical() throws Exception {
        NotificationEvent event = NotificationEvent.builder()
                .sourceService("direct-normal-service")
                .notificationType("DIRECT_NORMAL")
                .title("Direct Normal Message")
                .content("This is a direct normal message.")
                .priority(NotificationPriority.HIGH) // Non-CRITICAL priority
                .targetUserIds(Collections.singletonList("user1"))
                .build();

        mockMvc.perform(post("/api/notifications")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isAccepted());

        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        ArgumentCaptor<Boolean> criticalFlagCaptor = ArgumentCaptor.forClass(Boolean.class);
        verify(notificationProcessingOrchestrator).processNotification(eventCaptor.capture(), criticalFlagCaptor.capture());

        assertEquals(NotificationPriority.HIGH, eventCaptor.getValue().getPriority());
        assertFalse(criticalFlagCaptor.getValue());
    }

    @Test
    @WithMockUser(roles = "USER") // Or ADMIN, depending on who calls /critical
    void sendCriticalNotificationEndpoint_whenPriorityCritical_callsOrchestratorAsCritical() throws Exception {
        NotificationEvent event = NotificationEvent.builder()
                .sourceService("critical-endpoint-service")
                .notificationType("CRITICAL_ENDPOINT_ALERT")
                .title("Critical Endpoint Alert")
                .content("This is from /critical endpoint with CRITICAL priority.")
                .priority(NotificationPriority.CRITICAL)
                .targetUserIds(Collections.singletonList("user1"))
                .build();

        mockMvc.perform(post("/api/notifications/critical")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isAccepted());

        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        ArgumentCaptor<Boolean> criticalFlagCaptor = ArgumentCaptor.forClass(Boolean.class);
        verify(notificationProcessingOrchestrator).processNotification(eventCaptor.capture(), criticalFlagCaptor.capture());

        assertEquals(NotificationPriority.CRITICAL, eventCaptor.getValue().getPriority());
        assertTrue(criticalFlagCaptor.getValue());
    }

    @Test
    @WithMockUser(roles = "USER") // Or ADMIN
    void sendCriticalNotificationEndpoint_whenPriorityNotCritical_callsOrchestratorAsNotCritical() throws Exception {
        NotificationEvent event = NotificationEvent.builder()
                .sourceService("critical-endpoint-service-normal-prio")
                .notificationType("CRITICAL_ENDPOINT_NORMAL")
                .title("Critical Endpoint - Normal Priority")
                .content("This is from /critical endpoint but with MEDIUM priority.")
                .priority(NotificationPriority.MEDIUM) // Non-CRITICAL priority
                .targetUserIds(Collections.singletonList("user1"))
                .build();

        mockMvc.perform(post("/api/notifications/critical")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isAccepted());

        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        ArgumentCaptor<Boolean> criticalFlagCaptor = ArgumentCaptor.forClass(Boolean.class);
        verify(notificationProcessingOrchestrator).processNotification(eventCaptor.capture(), criticalFlagCaptor.capture());

        assertEquals(NotificationPriority.MEDIUM, eventCaptor.getValue().getPriority());
        assertFalse(criticalFlagCaptor.getValue());
    }
}
