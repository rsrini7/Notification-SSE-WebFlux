package com.example.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling; // Added import
import org.springframework.cache.annotation.EnableCaching; // Added import

@SpringBootApplication
@EnableScheduling // Added annotation
@EnableCaching // Added annotation
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}