package id.baundang.invitation.config;

import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

@Configuration
public class CacheConfig {

    @Bean
    RedisCacheManagerBuilderCustomizer cacheManagerBuilderCustomizer() {
        RedisCacheConfiguration json = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        return builder -> builder
                .cacheDefaults(json)
                .withCacheConfiguration("invitations", json.entryTtl(Duration.ofMinutes(5)))
                .withCacheConfiguration("guestbooks",  json.entryTtl(Duration.ofMinutes(1)));
    }
}
