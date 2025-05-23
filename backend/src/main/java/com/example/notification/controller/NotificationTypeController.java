package com.example.notification.controller;

import com.example.notification.dto.NotificationTypeDTO;
import com.example.notification.model.NotificationType;
import com.example.notification.service.NotificationTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notification-types")
@RequiredArgsConstructor
public class NotificationTypeController {

    private final NotificationTypeService notificationTypeService;

    @GetMapping
    public ResponseEntity<List<NotificationTypeDTO>> getAllNotificationTypes() {
        List<NotificationType> types = notificationTypeService.getAllActiveNotificationTypes();
        List<NotificationTypeDTO> dtos = types.stream()
                .map(NotificationTypeDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{code}")
    public ResponseEntity<NotificationTypeDTO> getNotificationType(@PathVariable String code) {
        NotificationType type = notificationTypeService.getNotificationTypeByCode(code);
        return ResponseEntity.ok(NotificationTypeDTO.fromEntity(type));
    }

    @PostMapping
    public ResponseEntity<NotificationTypeDTO> createNotificationType(
            @RequestParam String code,
            @RequestParam String description) {
        NotificationType type = notificationTypeService.createNotificationType(code, description);
        return ResponseEntity.ok(NotificationTypeDTO.fromEntity(type));
    }

    @PutMapping("/{code}")
    public ResponseEntity<NotificationTypeDTO> updateNotificationType(
            @PathVariable String code,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Boolean active) {
        NotificationType type = notificationTypeService.updateNotificationType(code, description, active);
        return ResponseEntity.ok(NotificationTypeDTO.fromEntity(type));
    }

    @DeleteMapping("/{code}")
    public ResponseEntity<Void> deleteNotificationType(@PathVariable String code) {
        notificationTypeService.deleteNotificationType(code);
        return ResponseEntity.noContent().build();
    }
}
