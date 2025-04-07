package com.example.notification.service;

import com.example.notification.dto.NotificationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * Service for sending email notifications
 */
@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final UserService userService;

    public EmailService(JavaMailSender mailSender, UserService userService) {
        this.mailSender = mailSender;
        this.userService = userService;
    }

    /**
     * Send a notification email to a user
     * @param userId The user ID
     * @param notification The notification to send
     */
    public void sendNotificationEmail(String userId, NotificationResponse notification) {
        try {
            // In a real application, we would get the user's email from a user service
            String userEmail = userService.getUserEmail(userId);
            if (userEmail == null || userEmail.isEmpty()) {
                log.warn("Cannot send email notification: No email found for user {}", userId);
                return;
            }
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            
            helper.setTo(userEmail);
            helper.setSubject("[" + notification.getSourceService() + "] " + notification.getNotificationType());
            
            // Create HTML content for the email
            String htmlContent = buildEmailContent(notification);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            log.info("Sent email notification to user {} at {}", userId, userEmail);
        } catch (MessagingException e) {
            log.error("Failed to send email notification to user {}: {}", userId, e.getMessage(), e);
        }
    }
    
    /**
     * Build the HTML content for the email
     */
    private String buildEmailContent(NotificationResponse notification) {
        StringBuilder builder = new StringBuilder();
        builder.append("<html><body>");
        builder.append("<h2>Notification from ").append(notification.getSourceService()).append("</h2>");
        builder.append("<p><strong>Type:</strong> ").append(notification.getNotificationType()).append("</p>");
        builder.append("<p><strong>Priority:</strong> ").append(notification.getPriority()).append("</p>");
        builder.append("<p><strong>Time:</strong> ").append(notification.getCreatedAt()).append("</p>");
        builder.append("<div style='padding: 15px; border: 1px solid #ddd; border-radius: 5px;'>");
        builder.append(notification.getContent());
        builder.append("</div>");
        
        // Add metadata if available
        if (notification.getMetadata() != null) {
            builder.append("<h3>Additional Information</h3>");
            builder.append("<pre>").append(notification.getMetadata().toString()).append("</pre>");
        }
        
        builder.append("<p>This is an automated message. Please do not reply to this email.</p>");
        builder.append("</body></html>");
        
        return builder.toString();
    }
}