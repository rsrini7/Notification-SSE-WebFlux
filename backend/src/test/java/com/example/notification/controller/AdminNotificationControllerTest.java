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
    @WithMockUser(roles = "ADMIN") // Add mock user with ADMIN role
    void sendNotification_CriticalTrue() throws Exception {
        List<String> targetUserIds = Arrays.asList("user1", "user2");
        NotificationEvent event = NotificationEvent.builder()
                .title("Critical Alert")
                .content("System meltdown imminent!")
                .notificationType("SYSTEM_ALERT")
                .sourceService("monitoring-service")
                .targetUserIds(targetUserIds)
                .priority(NotificationPriority.HIGH)
                .isCritical(true)
                .build();

        mockMvc.perform(post("/api/admin/notifications/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(event))
                .with(csrf())) // Re-add CSRF token
                .andExpect(status().isAccepted())
                .andExpect(content().string(containsString("Notification request processed for " + targetUserIds.size() + " user(s). Critical: true")));

        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(notificationService).sendNotification(eventCaptor.capture());
        NotificationEvent capturedEvent = eventCaptor.getValue();

        assertTrue(capturedEvent.isCritical());
        assertEquals("Critical Alert", capturedEvent.getTitle());
        assertEquals(targetUserIds, capturedEvent.getTargetUserIds());
    }

    @Test
    @WithMockUser(roles = "ADMIN") // Add mock user with ADMIN role
    void sendNotification_CriticalFalse() throws Exception {
        List<String> targetUserIds = Collections.singletonList("user3");
        NotificationEvent event = NotificationEvent.builder()
                .title("Friendly Reminder")
                .content("Don't forget the meeting.")
                .notificationType("REMINDER")
                .sourceService("calendar-service")
                .targetUserIds(targetUserIds)
                .priority(NotificationPriority.MEDIUM)
                .isCritical(false) // Explicitly false
                .build();

        mockMvc.perform(post("/api/admin/notifications/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(event))
                .with(csrf())) // Re-add CSRF token
                .andExpect(status().isAccepted())
                .andExpect(content().string(containsString("Notification request processed for " + targetUserIds.size() + " user(s). Critical: false")));

        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(notificationService).sendNotification(eventCaptor.capture());
        NotificationEvent capturedEvent = eventCaptor.getValue();

        assertFalse(capturedEvent.isCritical());
        assertEquals("Friendly Reminder", capturedEvent.getTitle());
        assertEquals(targetUserIds, capturedEvent.getTargetUserIds());
    }

    @Test
    @WithMockUser(roles = "ADMIN") // Add mock user with ADMIN role
    void sendNotification_CriticalAbsentDefaultsToFalse() throws Exception {
        List<String> targetUserIds = Collections.singletonList("user4");
        // Constructing JSON string manually to omit isCritical
        String eventJson = "{"
                + "\"title\":\"Default Test\","
                + "\"content\":\"Content here\","
                + "\"notificationType\":\"DEFAULT\","
                + "\"sourceService\":\"test-service\","
                + "\"targetUserIds\":[\"user4\"],"
                + "\"priority\":\"LOW\""
                // isCritical is absent
                + "}";

        mockMvc.perform(post("/api/admin/notifications/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(eventJson)
                .with(csrf())) // Re-add CSRF token
                .andExpect(status().isAccepted())
                .andExpect(content().string(containsString("Notification request processed for " + targetUserIds.size() + " user(s). Critical: false")));

        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(notificationService).sendNotification(eventCaptor.capture());
        NotificationEvent capturedEvent = eventCaptor.getValue();

        assertFalse(capturedEvent.isCritical(), "isCritical should default to false when absent in JSON");
        assertEquals("Default Test", capturedEvent.getTitle());
        assertEquals(targetUserIds, capturedEvent.getTargetUserIds());
    }
}
