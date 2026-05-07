package com.ridesharing.project.config;

import com.ridesharing.project.listener.OfferExpiryListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

// Registers the Redis Pub/Sub listener container that powers the offer expiry mechanism.
// Creates a dedicated connection for the listener — this connection cannot be shared
// with RedisTemplate because a connection in subscription (blocking) mode cannot
// simultaneously execute regular Redis commands. Separating them prevents deadlocks
// and command-timeout errors under load.
// Interacts with: OfferExpiryListener (the handler), RedisConfig (the template connection).
@Configuration
public class RedisListenerConfig {

    // Creates a dedicated RedisMessageListenerContainer and registers OfferExpiryListener
    // on the keyevent channel that Redis uses to broadcast expired-key notifications.
    // Every time any key in Redis database 0 expires, Redis publishes its name to
    // "__keyevent@0__:expired". OfferExpiryListener filters for "offer:" prefixed keys
    // so only ride offer timeouts are processed — all other expiry events are ignored.
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            OfferExpiryListener offerExpiryListener) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();

        // Dedicated connection for subscription mode — never share with RedisTemplate
        container.setConnectionFactory(connectionFactory);

        // Subscribe to the expired-key keyevent channel for Redis database 0.
        // Database 0 is the default database used by all Spring Data Redis operations.
        container.addMessageListener(
                offerExpiryListener,
                new ChannelTopic("__keyevent@0__:expired")
        );

        return container;
    }
}
