package com.example.notification.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

public class NotificationMetrics {

    private final Counter notificationsSent;
    private final Counter criticalNotificationsSent;
    private final Counter broadcastNotificationsSent;
    private final Counter notificationsRead;
    private final Counter notificationsUnread;

    public NotificationMetrics(MeterRegistry registry) {
        this.notificationsSent = Counter.builder("notifications_sent_total")
                .description("Total notifications sent")
                .register(registry);

        this.criticalNotificationsSent = Counter.builder("notifications_sent_critical")
                .description("Total critical notifications sent")
                .register(registry);

        this.broadcastNotificationsSent = Counter.builder("notifications_sent_broadcast")
                .description("Total broadcast notifications sent")
                .register(registry);

        this.notificationsRead = Counter.builder("notifications_read_total")
                .description("Total notifications marked as read")
                .register(registry);

        this.notificationsUnread = Counter.builder("notifications_unread_total")
                .description("Total notifications marked as unread")
                .register(registry);
    }

    public void incrementNotificationsSent() {
        notificationsSent.increment();
    }

    public void incrementCriticalNotificationsSent() {
        criticalNotificationsSent.increment();
    }

    public void incrementBroadcastNotificationsSent() {
        broadcastNotificationsSent.increment();
    }

    public void incrementNotificationsRead() {
        notificationsRead.increment();
    }

    public void incrementNotificationsUnread() {
        notificationsUnread.increment();
    }
}
