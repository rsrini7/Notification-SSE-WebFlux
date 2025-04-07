package com.example.notification.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@Configuration
public class MetricsConfig {
    @Bean
    MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }
    
    @Bean
    NotificationMetrics notificationMetrics(MeterRegistry registry) {
        return new NotificationMetrics(registry);
    }
}
