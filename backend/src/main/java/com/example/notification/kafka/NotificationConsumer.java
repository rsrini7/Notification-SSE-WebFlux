package com.example.notification.kafka;

import com.example.notification.dto.InterPodNotificationEvent;
import com.example.notification.dto.NotificationEvent;
import com.example.notification.dto.NotificationResponse;
import com.example.notification.model.Notification;
import com.example.notification.model.NotificationPriority;
import com.example.notification.model.User;
import com.example.notification.repository.UserRepository;
import com.example.notification.service.EmailService;
import com.example.notification.service.NotificationPersistenceService;
import com.example.notification.service.SseEmitterManager;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;



import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
@Slf4j
public class NotificationConsumer {

    private final NotificationPersistenceService persistenceService;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final KafkaTemplate<String, InterPodNotificationEvent> interPodKafkaTemplate;
    private final Cache userSessionCache;
    private final Cache pendingNotificationsCache;

    @Value("${notification.kafka.topics.notifications}")
    private String notificationsTopic;

    @Value("${notification.kafka.topics.inter-pod-notifications}")
    private String interPodTopic;

    public NotificationConsumer(NotificationPersistenceService persistenceService,
                                EmailService emailService, UserRepository userRepository,
                                KafkaTemplate<String, InterPodNotificationEvent> interPodKafkaTemplate,
                                CacheManager cacheManager) {
        this.persistenceService = persistenceService;
        this.emailService = emailService;
        this.userRepository = userRepository;
        this.interPodKafkaTemplate = interPodKafkaTemplate;
        this.userSessionCache = cacheManager.getCache("UserSessionCache");
        this.pendingNotificationsCache = cacheManager.getCache("PendingNotificationsCache");
        Objects.requireNonNull(userSessionCache, "Cache 'UserSessionCache' must not be null");
        Objects.requireNonNull(pendingNotificationsCache, "Cache 'PendingNotificationsCache' must not be null");
    }

    @KafkaListener(topics = "${notification.kafka.topics.notifications}", 
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory")
    public void consume(NotificationEvent event) {
        log.info("Received notification event from topic {}: {}", notificationsTopic, event);
        try {
            List<String> targetUserIds = event.getTargetUserIds();
            if (targetUserIds.contains("ALL")) {
                List<User> allUsers = userRepository.findAll();
                targetUserIds = allUsers.stream().map(User::getUsername).toList();
            }

            for (String userId : targetUserIds) {
                Notification savedNotification = persistenceService.persistNotification(event, userId);
                NotificationResponse response = persistenceService.convertToResponse(savedNotification);

                // 2. Check user session in Cache
                String podId = userSessionCache.get(userId, String.class);

                if (podId != null) {
                    // 3a. User is ONLINE: Route to the correct pod via Kafka
                    log.info("User {} is ONLINE on pod {}. Routing via Kafka topic '{}'.", userId, podId, interPodTopic);
                    InterPodNotificationEvent routeEvent = new InterPodNotificationEvent(userId, response);
                    interPodKafkaTemplate.send(interPodTopic, podId, routeEvent); // Key by podId
                } else {
                    // 3b. User is OFFLINE: Store in pending cache
                    log.info("User {} is OFFLINE. Storing notification in PendingNotificationsCache.", userId);
                    List<NotificationResponse> pending = pendingNotificationsCache.get(userId, () -> new ArrayList<>());
                    if (pending != null) {
                        pending.add(response);
                        pendingNotificationsCache.put(userId, pending);
                    }
                }
                
                if (event.isSendEmail()) {
                    emailService.sendNotificationEmail(userId, response);
                }
            }
        } catch (Exception e) {
            log.error("Error processing notification event: {}", e.getMessage(), e);
            // DLQ logic
        }
    }
}