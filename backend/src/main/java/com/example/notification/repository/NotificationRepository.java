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

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    Page<Notification> findByUserIdAndReadStatus(String userId, NotificationStatus status, Pageable pageable);
    @Query("SELECT n FROM Notification n JOIN n.notificationType nt WHERE n.userId = :userId AND nt.typeCode = :typeCode")
    Page<Notification> findByUserIdAndNotificationType(@Param("userId") String userId, @Param("typeCode") String typeCode, Pageable pageable);
    
    @Query("SELECT n FROM Notification n JOIN n.notificationType nt WHERE n.userId = :userId AND nt.typeCode = :typeCode ORDER BY n.createdAt DESC")
    Page<Notification> findByUserIdAndNotificationTypeOrderByCreatedAtDesc(@Param("userId") String userId, @Param("typeCode") String typeCode, Pageable pageable);
    long countByUserIdAndReadStatus(String userId, NotificationStatus status);
    long countByReadStatus(NotificationStatus status);
    long countByPriority(NotificationPriority priority);
    long countByCreatedAtAfter(LocalDateTime dateTime);
    // Use Pageable for limiting results instead of TopN with parameter
    Page<Notification> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT nt.typeCode FROM NotificationType nt WHERE nt.active = true")
    List<String> findDistinctNotificationTypes();

    @Query("SELECT nt.typeCode, COUNT(n) FROM Notification n JOIN n.notificationType nt GROUP BY nt.typeCode")
    List<Object[]> countGroupByNotificationType();

    @Query("SELECT n.priority, COUNT(n) FROM Notification n GROUP BY n.priority")
    List<Object[]> countGroupByPriority();

    @Query(value = "SELECT n.* FROM notifications n WHERE n.user_id = :userId " +
           "AND (REGEXP_LIKE(n.title, :searchTermRegex, 'i') " +
           "OR REGEXP_LIKE(n.content, :searchTermRegex, 'i'))",
           countQuery = "SELECT count(n.id) FROM notifications n WHERE n.user_id = :userId " +
           "AND (REGEXP_LIKE(n.title, :searchTermRegex, 'i') " +
           "OR REGEXP_LIKE(n.content, :searchTermRegex, 'i'))",
           nativeQuery = true)
    Page<Notification> searchNotifications(@Param("userId") String userId,
                                           @Param("searchTermRegex") String searchTermRegex,
                                           Pageable pageable);
}
