package com.example.notification.repository;

import com.example.notification.model.Notification;
import com.example.notification.model.NotificationStatus;
import com.example.notification.model.NotificationPriority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    Page<Notification> findByUserIdAndReadStatus(String userId, NotificationStatus status, Pageable pageable);
    Page<Notification> findByUserIdAndNotificationType(String userId, String type, Pageable pageable);
    long countByUserIdAndReadStatus(String userId, NotificationStatus status);
    long countByReadStatus(NotificationStatus status);
    long countByPriority(NotificationPriority priority);
    long countByCreatedAtAfter(LocalDateTime dateTime);
    // Use Pageable for limiting results instead of TopN with parameter
    Page<Notification> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT DISTINCT n.notificationType FROM Notification n")
    List<String> findDistinctNotificationTypes();

    @Query("SELECT n.notificationType, COUNT(n) FROM Notification n GROUP BY n.notificationType")
    Map<String, Long> countGroupByNotificationType();

    @Query("SELECT n.priority, COUNT(n) FROM Notification n GROUP BY n.priority")
    Map<String, Long> countGroupByPriority();

    @Query("SELECT n FROM Notification n WHERE n.userId = :userId " +
           "AND (LOWER(n.content) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(n.notificationType) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Notification> searchNotifications(@Param("userId") String userId, 
                                         @Param("searchTerm") String searchTerm,
                                         Pageable pageable);
}
