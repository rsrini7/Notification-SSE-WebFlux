package com.example.notification.dto;

import com.example.notification.model.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationTypeDTO {
    private Long id;
    private String typeCode;
    private String description;
    private LocalDateTime createdAt;
    private boolean active;

    public static NotificationTypeDTO fromEntity(NotificationType type) {
        if (type == null) {
            return null;
        }
        return NotificationTypeDTO.builder()
                .id(type.getId())
                .typeCode(type.getTypeCode())
                .description(type.getDescription())
                .createdAt(type.getCreatedAt())
                .active(type.isActive())
                .build();
    }

    public NotificationType toEntity() {
        NotificationType type = new NotificationType();
        type.setTypeCode(this.typeCode);
        type.setDescription(this.description);
        type.setActive(this.active);
        return type;
    }
}
