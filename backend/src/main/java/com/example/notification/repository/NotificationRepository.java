package com.example.notification.repository;

import com.example.notification.model.Notification;
import com.example.notification.model.NotificationPriority;
import com.example.notification.model.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    // Find notifications for a specific user
    Page<Notification> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    
    // Find unread notifications for a specific user
    Page<Notification> findByUserIdAndReadStatus(String userId, NotificationStatus readStatus, Pageable pageable);
    
    // Find notifications by type
    Page<Notification> findByUserIdAndNotificationType(String userId, String notificationType, Pageable pageable);
    
    // Find notifications by priority
    Page<Notification> findByUserIdAndPriority(String userId, NotificationPriority priority, Pageable pageable);
    
    // Find notifications within a date range
    Page<Notification> findByUserIdAndCreatedAtBetween(
            String userId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    
    // Count unread notifications for a user
    long countByUserIdAndReadStatus(String userId, NotificationStatus readStatus);
    
    // Mark all notifications as read for a user
    @Modifying
    @Query("UPDATE Notification n SET n.readStatus = :status WHERE n.userId = :userId")
    int updateReadStatusForUser(@Param("userId") String userId, @Param("status") NotificationStatus status);
    
    // Search notifications by content (case insensitive)
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND LOWER(n.content) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Notification> searchByContent(@Param("userId") String userId, @Param("searchTerm") String searchTerm, Pageable pageable);
}