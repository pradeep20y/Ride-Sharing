package com.ridesharing.project.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

// Central Redis configuration for the ride-sharing application.
// Provides the shared RedisTemplate used by LocationService and RideMatchingService,
// and enables the keyspace notifications that drive the offer expiry mechanism.
// Interacts with: RedisListenerConfig (which consumes the keyspace events enabled here).
@Configuration
public class RedisConfig {

    // Configures a RedisTemplate that serialises every key, value, hash key, and hash value
    // as a plain UTF-8 string. StringRedisSerializer is chosen instead of the default
    // JdkSerializationRedisSerializer so that members stored by Redis GEO commands
    // (e.g. driver IDs) and offer keys (e.g. "offer:{requestId}") remain human-readable
    // in Redis and are directly compatible with native GEO and TTL operations.
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer serializer = new StringRedisSerializer();
        template.setKeySerializer(serializer);
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(serializer);
        template.setHashValueSerializer(serializer);

        return template;
    }

    // Enables Redis keyspace notification delivery for expired-key events.
    // "E" = keyspace events enabled (Redis will publish to __keyevent@*__ channels).
    // "x" = only expired-key events are published (not set, delete, etc.).
    // Without this configuration Redis silently discards expiry events and
    // OfferExpiryListener will never be triggered, making the 10-second offer
    // timeout completely non-functional.
    // InitializingBean runs during application startup before the first request
    // is served, guaranteeing notifications are active before any offer key is written.
    @Bean
    public InitializingBean enableRedisKeyspaceNotifications(RedisConnectionFactory connectionFactory) {
        return () -> connectionFactory.getConnection()
                .serverCommands()
                .setConfig("notify-keyspace-events", "Ex");
    }
}
