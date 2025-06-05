package com.example.notification.controller;

import com.example.notification.dto.NotificationEvent;
import com.example.notification.model.NotificationPriority;
import com.example.notification.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf; // Re-import csrf
import org.springframework.security.test.context.support.WithMockUser; // Import WithMockUser

@WebMvcTest(AdminNotificationController.class)
public class AdminNotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private com.example.notification.security.JwtTokenProvider jwtTokenProvider; // Added mock for JwtTokenProvider

    @Test
    @WithMockUser(roles = "ADMIN")
    void sendNotification_whenPriorityCritical_invokesOrchestratorAsCritical() throws Exception {
        List<String> targetUserIds = Arrays.asList("user1", "user2");
        NotificationEvent event = NotificationEvent.builder()
                .title("Critical Alert")
                .content("System meltdown imminent!")
                .notificationType("SYSTEM_ALERT")
                .sourceService("monitoring-service")
                .targetUserIds(targetUserIds)
                .priority(NotificationPriority.CRITICAL) // Set to CRITICAL
                // isCritical field removed
                .build();

        mockMvc.perform(post("/api/admin/notifications/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(event))
                .with(csrf()))
                .andExpect(status().isAccepted())
                .andExpect(content().string(containsString("Notification request processed for " + targetUserIds.size() + " user(s). Priority: CRITICAL")));

        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(notificationService).sendNotification(eventCaptor.capture());
        NotificationEvent capturedEvent = eventCaptor.getValue();

        assertEquals(NotificationPriority.CRITICAL, capturedEvent.getPriority());
        assertEquals("Critical Alert", capturedEvent.getTitle());
        assertEquals(targetUserIds, capturedEvent.getTargetUserIds());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void sendNotification_whenPriorityNotCritical_invokesOrchestratorAsNotCritical() throws Exception {
        List<String> targetUserIds = Collections.singletonList("user3");
        NotificationEvent event = NotificationEvent.builder()
                .title("Friendly Reminder")
                .content("Don't forget the meeting.")
                .notificationType("REMINDER")
                .sourceService("calendar-service")
                .targetUserIds(targetUserIds)
                .priority(NotificationPriority.MEDIUM) // Set to non-CRITICAL
                // isCritical field removed
                .build();

        mockMvc.perform(post("/api/admin/notifications/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(event))
                .with(csrf()))
                .andExpect(status().isAccepted())
                .andExpect(content().string(containsString("Notification request processed for " + targetUserIds.size() + " user(s). Priority: MEDIUM")));

        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(notificationService).sendNotification(eventCaptor.capture());
        NotificationEvent capturedEvent = eventCaptor.getValue();

        assertEquals(NotificationPriority.MEDIUM, capturedEvent.getPriority());
        assertEquals("Friendly Reminder", capturedEvent.getTitle());
        assertEquals(targetUserIds, capturedEvent.getTargetUserIds());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void sendNotification_whenPriorityAbsentInJson_passesPriorityAsNullToService() throws Exception {
        List<String> targetUserIds = Collections.singletonList("user4");
        // Constructing JSON string manually to omit priority (and isCritical)
        String eventJson = "{"
                + "\"title\":\"Default Test\","
                + "\"content\":\"Content here\","
                + "\"notificationType\":\"DEFAULT\","
                + "\"sourceService\":\"test-service\","
                + "\"targetUserIds\":[\"user4\"]"
                // priority is absent
                + "}";

        mockMvc.perform(post("/api/admin/notifications/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(eventJson)
                .with(csrf()))
                .andExpect(status().isAccepted())
                // The controller will display "Priority: null"
                .andExpect(content().string(containsString("Notification request processed for " + targetUserIds.size() + " user(s). Priority: null")));

        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(notificationService).sendNotification(eventCaptor.capture());
        NotificationEvent capturedEvent = eventCaptor.getValue();

        assertEquals(null, capturedEvent.getPriority(), "Priority should be null when absent in JSON");
        assertEquals("Default Test", capturedEvent.getTitle());
        assertEquals(targetUserIds, capturedEvent.getTargetUserIds());
    }
}
