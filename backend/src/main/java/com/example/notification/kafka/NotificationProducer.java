package com.example.notification.kafka;

import com.example.notification.dto.NotificationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationProducer {

    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    @Value("${notification.kafka.topics.notifications}")
    private String notificationsTopic;

    public NotificationProducer(KafkaTemplate<String, NotificationEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendNotificationEvent(NotificationEvent event) {
        kafkaTemplate.send(notificationsTopic, event);
        log.info("Sent notification event to topic {}: {}", notificationsTopic, event);
    }
}
