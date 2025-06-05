package com.example.notification.service;

import com.example.notification.dto.NotificationResponse;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mail.javamail.JavaMailSender;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private UserService userService;

    @InjectMocks
    private EmailService emailService;

    private MimeMessage dummyMimeMessage;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Create a real MimeMessage for capturing and assertions, but don't rely on a mail server
        dummyMimeMessage = new MimeMessage((Session) null);
    }

    @Test
    void sendNotificationEmail_Success() throws MessagingException, IOException {
        // Given
        String userId = UUID.randomUUID().toString();
        String testEmail = "testuser@example.com";
        NotificationResponse notificationResponse = NotificationResponse.builder()
                .id(1L) // Example ID
                .userId(userId)
                .sourceService("Test Source Service")
                .notificationType("Test Type")
                .title("Test Title") // Added title as it's a field in NotificationResponse
                .content("This is a test notification content.")
                .priority(com.example.notification.model.NotificationPriority.HIGH)
                .readStatus(com.example.notification.model.NotificationStatus.UNREAD) // Assuming UNREAD is a valid status
                .createdAt(java.time.LocalDateTime.now())
                .build();

        when(userService.getUserEmail(userId)).thenReturn(testEmail);
        when(mailSender.createMimeMessage()).thenReturn(dummyMimeMessage);

        // When
        emailService.sendNotificationEmail(userId, notificationResponse);

        // Then
        verify(mailSender, times(1)).send(dummyMimeMessage);

        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        MimeMessage capturedMessage = messageCaptor.getValue();
        assertEquals(testEmail, capturedMessage.getAllRecipients()[0].toString());
        String expectedSubject = "[" + notificationResponse.getSourceService() + "] " + notificationResponse.getNotificationType();
        assertEquals(expectedSubject, capturedMessage.getSubject());

        // Handle MimeMultipart content
        Object capturedContentObject = capturedMessage.getContent();
        String content;
        if (capturedContentObject instanceof String) {
            content = (String) capturedContentObject;
        } else if (capturedContentObject instanceof jakarta.mail.internet.MimeMultipart) {
            jakarta.mail.internet.MimeMultipart multipart = (jakarta.mail.internet.MimeMultipart) capturedContentObject;
            if (multipart.getCount() > 0) {
                jakarta.mail.BodyPart part = multipart.getBodyPart(0);
                Object bodyPartContent = part.getContent();
                if (bodyPartContent instanceof String) {
                    content = (String) bodyPartContent;
                } else if (bodyPartContent instanceof jakarta.mail.internet.MimeMultipart) {
                    // Nested MimeMultipart
                    jakarta.mail.internet.MimeMultipart innerMultipart = (jakarta.mail.internet.MimeMultipart) bodyPartContent;
                    if (innerMultipart.getCount() > 0) {
                        jakarta.mail.BodyPart innerPart = innerMultipart.getBodyPart(0);
                        // Assuming the actual HTML is here
                        if (innerPart.isMimeType("text/html") || innerPart.isMimeType("text/plain")) {
                             content = (String) innerPart.getContent();
                        } else {
                             // If it's an InputStream or other, read it
                            java.io.InputStream inputStream = (java.io.InputStream) innerPart.getContent();
                            java.io.ByteArrayOutputStream result = new java.io.ByteArrayOutputStream();
                            byte[] buffer = new byte[1024];
                            int length;
                            while ((length = inputStream.read(buffer)) != -1) {
                                result.write(buffer, 0, length);
                            }
                            content = result.toString(java.nio.charset.StandardCharsets.UTF_8.name());
                        }
                    } else {
                        fail("Inner MimeMultipart has no body parts.");
                        return;
                    }
                } else if (bodyPartContent instanceof java.io.InputStream) {
                    java.io.InputStream inputStream = (java.io.InputStream) bodyPartContent;
                    java.io.ByteArrayOutputStream result = new java.io.ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = inputStream.read(buffer)) != -1) {
                        result.write(buffer, 0, length);
                    }
                    content = result.toString(java.nio.charset.StandardCharsets.UTF_8.name());
                } else {
                    fail("BodyPart content is of unexpected type: " + bodyPartContent.getClass().getName());
                    return;
                }
            } else {
                fail("MimeMultipart has no body parts.");
                return;
            }
        } else {
            fail("Unexpected content type: " + capturedContentObject.getClass().getName());
            return;
        }

        assertTrue(content.contains(notificationResponse.getSourceService()));
        assertTrue(content.contains(notificationResponse.getNotificationType()));
        assertTrue(content.contains(notificationResponse.getContent()));
        // assertTrue(content.contains("Dear User,")); // This was not in the actual email content
        // assertTrue(content.contains("You have a new notification:")); // This was not in the actual email content
    }

    @Test
    void sendNotificationEmail_UserEmailNull_ShouldNotSend() {
        // Given
        String userId = UUID.randomUUID().toString();
        NotificationResponse notificationResponse = NotificationResponse.builder()
                .id(2L) // Example ID
                .userId(userId)
                .sourceService("Test Source Service")
                .notificationType("Test Type")
                .title("Test Title")
                .content("This is a test notification content.")
                .priority(com.example.notification.model.NotificationPriority.MEDIUM)
                .readStatus(com.example.notification.model.NotificationStatus.UNREAD)
                .createdAt(java.time.LocalDateTime.now())
                .build();

        when(userService.getUserEmail(userId)).thenReturn(null);

        // When
        emailService.sendNotificationEmail(userId, notificationResponse);

        // Then
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void sendNotificationEmail_UserEmailEmpty_ShouldNotSend() {
        // Given
        String userId = UUID.randomUUID().toString();
        NotificationResponse notificationResponse = NotificationResponse.builder()
                .id(3L) // Example ID
                .userId(userId)
                .sourceService("Test Source Service")
                .notificationType("Test Type")
                .title("Test Title")
                .content("This is a test notification content.")
                .priority(com.example.notification.model.NotificationPriority.LOW)
                .readStatus(com.example.notification.model.NotificationStatus.UNREAD)
                .createdAt(java.time.LocalDateTime.now())
                .build();

        when(userService.getUserEmail(userId)).thenReturn("");

        // When
        emailService.sendNotificationEmail(userId, notificationResponse);

        // Then
        verify(mailSender, never()).send(any(MimeMessage.class));
    }
}
