package com.ridesharing.project.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    // Configures a RedisTemplate that serialises every key, value, hash key, and hash value
    // as a plain UTF-8 string.  StringRedisSerializer is chosen instead of the default
    // JdkSerializationRedisSerializer so that members stored by the Redis GEO commands
    // (e.g., driver IDs) remain human-readable in Redis and are directly compatible with
    // native GEOADD / GEODIST / GEORADIUS commands — those commands treat members as byte
    // strings, so a Java-serialised blob would be an unreadable, unusable member name.
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
}
