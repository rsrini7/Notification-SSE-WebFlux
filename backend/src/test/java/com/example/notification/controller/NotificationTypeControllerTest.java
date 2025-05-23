package com.example.notification.controller;

import com.example.notification.dto.NotificationTypeDTO;
import com.example.notification.model.NotificationType;
import com.example.notification.service.NotificationTypeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationTypeControllerTest {

    @Mock
    private NotificationTypeService notificationTypeService;

    @InjectMocks
    private NotificationTypeController notificationTypeController;

    private NotificationType accountType;
    private NotificationType paymentType;
    private NotificationTypeDTO accountTypeDTO;
    private NotificationTypeDTO paymentTypeDTO;

    @BeforeEach
    void setUp() {
        accountType = new NotificationType();
        accountType.setId(1L);
        accountType.setTypeCode("ACCOUNT");
        accountType.setDescription("Account related notifications");
        accountType.setActive(true);
        accountType.setCreatedAt(LocalDateTime.now());

        paymentType = new NotificationType();
        paymentType.setId(2L);
        paymentType.setTypeCode("PAYMENT");
        paymentType.setDescription("Payment related notifications");
        paymentType.setActive(true);
        paymentType.setCreatedAt(LocalDateTime.now());

        accountTypeDTO = NotificationTypeDTO.builder()
                .id(1L)
                .typeCode("ACCOUNT")
                .description("Account related notifications")
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        paymentTypeDTO = NotificationTypeDTO.builder()
                .id(2L)
                .typeCode("PAYMENT")
                .description("Payment related notifications")
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void getAllNotificationTypes_ShouldReturnAllActiveTypes() {
        // Arrange
        when(notificationTypeService.getAllActiveNotificationTypes())
                .thenReturn(Arrays.asList(accountType, paymentType));

        // Act
        ResponseEntity<List<NotificationTypeDTO>> response = notificationTypeController.getAllNotificationTypes();

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals("ACCOUNT", response.getBody().get(0).getTypeCode());
        assertEquals("PAYMENT", response.getBody().get(1).getTypeCode());
        verify(notificationTypeService, times(1)).getAllActiveNotificationTypes();
    }

    @Test
    void getNotificationType_WhenExists_ShouldReturnType() {
        // Arrange
        when(notificationTypeService.getNotificationTypeByCode("ACCOUNT")).thenReturn(accountType);

        // Act
        ResponseEntity<NotificationTypeDTO> response = notificationTypeController.getNotificationType("ACCOUNT");

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("ACCOUNT", response.getBody().getTypeCode());
        verify(notificationTypeService, times(1)).getNotificationTypeByCode("ACCOUNT");
    }

    @Test
    void createNotificationType_WhenValid_ShouldCreateType() {
        // Arrange
        when(notificationTypeService.createNotificationType("NEW_TYPE", "New Type Description"))
                .thenReturn(accountType);

        // Act
        ResponseEntity<NotificationTypeDTO> response = notificationTypeController.createNotificationType(
                "NEW_TYPE", "New Type Description");

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(notificationTypeService, times(1))
                .createNotificationType("NEW_TYPE", "New Type Description");
    }

    @Test
    void updateNotificationType_WhenValid_ShouldUpdateType() {
        // Arrange
        when(notificationTypeService.updateNotificationType("ACCOUNT", "Updated Description", false))
                .thenReturn(accountType);

        // Act
        ResponseEntity<NotificationTypeDTO> response = notificationTypeController.updateNotificationType(
                "ACCOUNT", "Updated Description", false);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(notificationTypeService, times(1))
                .updateNotificationType("ACCOUNT", "Updated Description", false);
    }

    @Test
    void deleteNotificationType_WhenExists_ShouldDeleteType() {
        // Arrange
        doNothing().when(notificationTypeService).deleteNotificationType("ACCOUNT");

        // Act
        ResponseEntity<Void> response = notificationTypeController.deleteNotificationType("ACCOUNT");
        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(notificationTypeService, times(1)).deleteNotificationType("ACCOUNT");
    }
}
