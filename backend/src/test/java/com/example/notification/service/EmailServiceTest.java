package com.example.notification.service;

import com.example.notification.dto.NotificationResponse;
import com.example.notification.model.UserPreferences;
import com.example.notification.repository.UserPreferencesRepository;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private UserService userService;

    @Mock
    private UserPreferencesRepository userPreferencesRepository;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private EmailService emailService;

    private NotificationResponse notificationResponse;
    private UserPreferences userPreferences;

    @BeforeEach
    void setUp() {
        notificationResponse = new NotificationResponse();
        notificationResponse.setNotificationType("TEST_NOTIFICATION");
        notificationResponse.setContent("Test content");
        notificationResponse.setSourceService("TestService");

        userPreferences = new UserPreferences();
        userPreferences.setUserId("testUser");
        userPreferences.setEmailEnabled(true);
        userPreferences.setMutedNotificationTypes(Collections.emptySet());

        // Mock the behavior of mailSender.createMimeMessage()
        // This is important because the actual mailSender.createMimeMessage() would return null in a test environment
        // and this would cause a NullPointerException when MimeMessageHelper is instantiated.
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    @Test
    void testSendNotificationEmail_EmailDisabled() {
        userPreferences.setEmailEnabled(false);
        when(userPreferencesRepository.findByUserId("testUser")).thenReturn(Optional.of(userPreferences));

        emailService.sendNotificationEmail("testUser", notificationResponse);

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void testSendNotificationEmail_NotificationTypeMuted() {
        userPreferences.setMutedNotificationTypes(Set.of("TEST_NOTIFICATION"));
        when(userPreferencesRepository.findByUserId("testUser")).thenReturn(Optional.of(userPreferences));

        emailService.sendNotificationEmail("testUser", notificationResponse);

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void testSendNotificationEmail_EmailEnabledAndNotMuted() {
        when(userPreferencesRepository.findByUserId("testUser")).thenReturn(Optional.of(userPreferences));
        when(userService.getUserEmail("testUser")).thenReturn("test@example.com");

        emailService.sendNotificationEmail("testUser", notificationResponse);

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void testSendNotificationEmail_UserPreferencesNotFound() {
        when(userPreferencesRepository.findByUserId("testUser")).thenReturn(Optional.empty());

        emailService.sendNotificationEmail("testUser", notificationResponse);

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void testSendNotificationEmail_UserEmailNotFound() {
        when(userPreferencesRepository.findByUserId("testUser")).thenReturn(Optional.of(userPreferences));
        when(userService.getUserEmail("testUser")).thenReturn(null);

        emailService.sendNotificationEmail("testUser", notificationResponse);

        verify(mailSender, never()).send(any(MimeMessage.class));
    }
}
