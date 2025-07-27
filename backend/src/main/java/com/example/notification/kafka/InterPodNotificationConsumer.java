package com.example.notification.kafka;

import com.example.notification.dto.InterPodNotificationEvent;
import com.example.notification.service.SseEmitterManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class InterPodNotificationConsumer {

    private final SseEmitterManager sseEmitterManager;

    @Value("${pod.hostname}")
    private String podHostname;

    public InterPodNotificationConsumer(SseEmitterManager sseEmitterManager) {
        this.sseEmitterManager = sseEmitterManager;
    }

    @KafkaListener(topics = "${notification.kafka.topics.inter-pod-notifications}",
                   containerFactory = "interPodKafkaListenerContainerFactory") // Needs a specific container factory
    public void consume(ConsumerRecord<String, InterPodNotificationEvent> record) {
        String targetPodId = record.key();
        InterPodNotificationEvent event = record.value();

        // This pod should only process messages keyed with its own hostname.
        if (podHostname.equals(targetPodId)) {
            log.info("Pod '{}' received routed notification for user '{}'. Pushing to SSE.", podHostname, event.getTargetUserId());
            sseEmitterManager.sendToUser(event.getTargetUserId(), event.getPayload());
        }
        // If the key does not match, this message is for another pod instance. Ignore it.
    }
}