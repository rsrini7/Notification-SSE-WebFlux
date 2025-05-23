package com.example.notification.repository;

import com.example.notification.model.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationTypeRepository extends JpaRepository<NotificationType, Long> {
    Optional<NotificationType> findByTypeCode(String typeCode);
    List<NotificationType> findByActiveTrue();
}
