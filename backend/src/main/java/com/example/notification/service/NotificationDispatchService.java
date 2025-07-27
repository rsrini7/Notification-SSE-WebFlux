package com.example.notification.service;

import com.example.notification.dto.NotificationEvent;
import com.example.notification.kafka.NotificationProducer;
import com.example.notification.model.NotificationPriority;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationDispatchService {

    private final NotificationProducer notificationProducer;

    public void dispatchNotification(NotificationEvent event) {
        notificationProducer.sendNotificationEvent(event);
        log.info("Dispatched notification event to Kafka: {}", event);
    }
}