package com.example.notification.config;

import com.example.notification.dto.NotificationResponse;
import com.example.notification.model.User;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.gemfire.config.annotation.ClientCacheApplication;
import org.springframework.data.gemfire.config.annotation.EnableCachingDefinedRegions;
import org.springframework.data.gemfire.repository.config.EnableGemfireRepositories;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.ClientRegionShortcut;
import java.util.List;

@Configuration
@ClientCacheApplication
@EnableCachingDefinedRegions(clientRegionShortcut = ClientRegionShortcut.CACHING_PROXY)
@EnableGemfireRepositories(basePackageClasses = {User.class}) // Example if you had Gemfire repos
public class GemfireConfig {

    @Bean("UserSessionCache")
    public org.apache.geode.cache.Region<String, String> userSessionCache(ClientCache clientCache) {
        return clientCache.<String, String>createClientRegionFactory(ClientRegionShortcut.CACHING_PROXY)
            .create("UserSessionCache");
    }

    @Bean("PendingNotificationsCache")
 public org.apache.geode.cache.Region<String, List<NotificationResponse>> pendingNotificationsCache(ClientCache clientCache) {
        return clientCache.<String, List<NotificationResponse>>createClientRegionFactory(ClientRegionShortcut.CACHING_PROXY)
            .create("PendingNotificationsCache");
    }
}