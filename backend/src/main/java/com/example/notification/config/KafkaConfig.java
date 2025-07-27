package com.example.notification.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Value("${notification.kafka.topics.notifications}")
    private String notificationsTopic;
    
    @Value("${notification.kafka.topics.critical-notifications}")
    private String criticalNotificationsTopic;
     
    // Create the standard notifications topic
    @Bean
    public NewTopic notificationsTopic() {
        return TopicBuilder.name(notificationsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
    
    // Create the critical notifications topic
    @Bean
    public NewTopic criticalNotificationsTopic() {
        return TopicBuilder.name(criticalNotificationsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
 
}