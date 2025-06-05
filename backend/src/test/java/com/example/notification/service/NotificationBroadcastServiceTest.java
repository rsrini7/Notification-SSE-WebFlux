package com.example.notification.service;

import com.example.notification.dto.NotificationEvent;
import com.example.notification.dto.NotificationResponse;
import com.example.notification.model.Notification;
import com.example.notification.model.NotificationPriority;
import com.example.notification.model.NotificationType;
import com.example.notification.model.User;
import com.example.notification.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NotificationBroadcastServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationPersistenceService persistenceService;

    @Mock
    private NotificationDispatchService dispatchService;

    @InjectMocks
    private NotificationBroadcastService notificationBroadcastService;

    private User user1, user2;
    private Notification notification1, notification2;
    private NotificationResponse response1, response2;
    private NotificationType mockNotificationType;

    @BeforeEach
    void setUp() {
        user1 = new User();
        user1.setUsername("user1"); // ID field is username
        user1.setEmail("user1@example.com");

        user2 = new User();
        user2.setUsername("user2"); // ID field is username
        user2.setEmail("user2@example.com");

        mockNotificationType = new NotificationType();
        mockNotificationType.setTypeCode("BROADCAST_TYPE");
        mockNotificationType.setDescription("A broadcast type");
        mockNotificationType.setId(1L);


        notification1 = Notification.builder()
                .id(1L)
                .userId(user1.getUsername()) // Use getUsername()
                .title("Broadcast Title")
                .content("Broadcast Content")
                .notificationType(mockNotificationType)
                .sourceService("test-service")
                .createdAt(LocalDateTime.now())
                .build();
        notification2 = Notification.builder()
                .id(2L)
                .userId(user2.getUsername()) // Use getUsername()
                .title("Broadcast Title")
                .content("Broadcast Content")
                .notificationType(mockNotificationType)
                .sourceService("test-service")
                .createdAt(LocalDateTime.now())
                .build();

        response1 = NotificationResponse.builder().id(1L).userId(user1.getUsername()).title("Broadcast Title").content("Broadcast Content").notificationType("BROADCAST_TYPE").build();
        response2 = NotificationResponse.builder().id(2L).userId(user2.getUsername()).title("Broadcast Title").content("Broadcast Content").notificationType("BROADCAST_TYPE").build();
    }

    @Test
    void processBroadcast_whenPriorityCritical_sendsEmailsAndSSEs() {
        NotificationEvent event = NotificationEvent.builder()
                .title("Critical Broadcast")
                .content("This is a critical broadcast message.")
                .notificationType("SYSTEM_ALERT")
                .sourceService("emergency-service")
                .priority(NotificationPriority.CRITICAL) // Set to CRITICAL
                // isCritical field removed
                .build();

        List<User> users = Arrays.asList(user1, user2);
        List<Notification> savedNotifications = Arrays.asList(notification1, notification2);

        when(userRepository.findAll()).thenReturn(users);
        when(persistenceService.persistBroadcastNotifications(event, users)).thenReturn(savedNotifications);
        when(persistenceService.convertToResponse(notification1)).thenReturn(response1);
        when(persistenceService.convertToResponse(notification2)).thenReturn(response2);

        notificationBroadcastService.processBroadcast(event);

        verify(dispatchService, times(2)).dispatchNotification(anyString(), any(NotificationResponse.class)); // SSE
        verify(dispatchService, times(2)).dispatchToEmail(anyString(), any(NotificationResponse.class));    // Email
    }

    @Test
    void processBroadcast_whenPriorityNotCritical_sendsOnlySSEs() {
        NotificationEvent event = NotificationEvent.builder()
                .title("Normal Broadcast")
                .content("This is a normal broadcast message.")
                .notificationType("INFO_UPDATE")
                .sourceService("info-service")
                .priority(NotificationPriority.MEDIUM) // Set to non-CRITICAL (e.g., MEDIUM)
                // isCritical field removed
                .build();

        List<User> users = Arrays.asList(user1, user2);
        List<Notification> savedNotifications = Arrays.asList(notification1, notification2);

        when(userRepository.findAll()).thenReturn(users);
        when(persistenceService.persistBroadcastNotifications(event, users)).thenReturn(savedNotifications);
        when(persistenceService.convertToResponse(notification1)).thenReturn(response1);
        when(persistenceService.convertToResponse(notification2)).thenReturn(response2);

        notificationBroadcastService.processBroadcast(event);

        verify(dispatchService, times(2)).dispatchNotification(anyString(), any(NotificationResponse.class)); // SSE
        verify(dispatchService, never()).dispatchToEmail(anyString(), any(NotificationResponse.class));        // No Email
    }

    @Test
    void processBroadcast_whenNoUsers_doesNothing() {
        NotificationEvent event = NotificationEvent.builder()
                .title("Test Broadcast")
                .content("Content")
                .notificationType("TEST_TYPE")
                .sourceService("test-service")
                .priority(NotificationPriority.LOW)
                // isCritical field removed
                .build();

        when(userRepository.findAll()).thenReturn(Collections.emptyList());

        notificationBroadcastService.processBroadcast(event);

        verifyNoInteractions(persistenceService);
        verifyNoInteractions(dispatchService);
    }
}
