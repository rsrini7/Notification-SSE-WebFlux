package com.example.notification.service;

import com.example.notification.model.NotificationType;
import com.example.notification.repository.NotificationTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationTypeServiceTest {

    @Mock
    private NotificationTypeRepository notificationTypeRepository;

    @InjectMocks
    private NotificationTypeService notificationTypeService;

    private NotificationType accountType;
    private NotificationType paymentType;

    @BeforeEach
    void setUp() {
        accountType = new NotificationType();
        accountType.setId(1L);
        accountType.setTypeCode("ACCOUNT");
        accountType.setDescription("Account related notifications");
        accountType.setActive(true);

        paymentType = new NotificationType();
        paymentType.setId(2L);
        paymentType.setTypeCode("PAYMENT");
        paymentType.setDescription("Payment related notifications");
        paymentType.setActive(true);
    }

    @Test
    void getAllActiveNotificationTypes_ShouldReturnActiveTypes() {
        // Arrange
        when(notificationTypeRepository.findByActiveTrue()).thenReturn(Arrays.asList(accountType, paymentType));

        // Act
        List<NotificationType> result = notificationTypeService.getAllActiveNotificationTypes();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("ACCOUNT", result.get(0).getTypeCode());
        assertEquals("PAYMENT", result.get(1).getTypeCode());
        verify(notificationTypeRepository, times(1)).findByActiveTrue();
    }

    @Test
    void getNotificationTypeByCode_WhenExists_ShouldReturnType() {
        // Arrange
        when(notificationTypeRepository.findByTypeCode("ACCOUNT")).thenReturn(Optional.of(accountType));

        // Act
        NotificationType result = notificationTypeService.getNotificationTypeByCode("ACCOUNT");

        // Assert
        assertNotNull(result);
        assertEquals("ACCOUNT", result.getTypeCode());
        verify(notificationTypeRepository, times(1)).findByTypeCode("ACCOUNT");
    }

    @Test
    void getNotificationTypeByCode_WhenNotExists_ShouldThrowException() {
        // Arrange
        when(notificationTypeRepository.findByTypeCode("INVALID")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            notificationTypeService.getNotificationTypeByCode("INVALID");
        });
        verify(notificationTypeRepository, times(1)).findByTypeCode("INVALID");
    }

    @Test
    void createNotificationType_WhenNew_ShouldCreateType() {
        // Arrange
        when(notificationTypeRepository.findByTypeCode("NEW_TYPE")).thenReturn(Optional.empty());
        when(notificationTypeRepository.save(any(NotificationType.class))).thenAnswer(invocation -> {
            NotificationType type = invocation.getArgument(0);
            type.setId(3L);
            return type;
        });

        // Act
        NotificationType result = notificationTypeService.createNotificationType("NEW_TYPE", "New Type Description");
        // Assert
        assertNotNull(result);
        assertEquals("NEW_TYPE", result.getTypeCode());
        assertEquals("New Type Description", result.getDescription());
        assertTrue(result.isActive());
        verify(notificationTypeRepository, times(1)).findByTypeCode("NEW_TYPE");
        verify(notificationTypeRepository, times(1)).save(any(NotificationType.class));
    }

    @Test
    void createNotificationType_WhenExists_ShouldThrowException() {
        // Arrange
        when(notificationTypeRepository.findByTypeCode("EXISTING")).thenReturn(Optional.of(accountType));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            notificationTypeService.createNotificationType("EXISTING", "Existing Type");
        });
        verify(notificationTypeRepository, times(1)).findByTypeCode("EXISTING");
        verify(notificationTypeRepository, never()).save(any(NotificationType.class));
    }

    @Test
    void updateNotificationType_WhenExists_ShouldUpdateType() {
        // Arrange
        when(notificationTypeRepository.findByTypeCode("ACCOUNT")).thenReturn(Optional.of(accountType));
        when(notificationTypeRepository.save(any(NotificationType.class))).thenReturn(accountType);

        // Act
        NotificationType result = notificationTypeService.updateNotificationType("ACCOUNT", "Updated Description", false);


        // Assert
        assertNotNull(result);
        assertEquals("ACCOUNT", result.getTypeCode());
        assertEquals("Updated Description", result.getDescription());
        assertFalse(result.isActive());
        verify(notificationTypeRepository, times(1)).findByTypeCode("ACCOUNT");
        verify(notificationTypeRepository, times(1)).save(accountType);
    }

    @Test
    void deleteNotificationType_WhenExists_ShouldDeleteType() {
        // Arrange
        when(notificationTypeRepository.findByTypeCode("ACCOUNT")).thenReturn(Optional.of(accountType));
        doNothing().when(notificationTypeRepository).delete(accountType);

        // Act
        notificationTypeService.deleteNotificationType("ACCOUNT");


        // Assert
        verify(notificationTypeRepository, times(1)).findByTypeCode("ACCOUNT");
        verify(notificationTypeRepository, times(1)).delete(accountType);
    }
}
