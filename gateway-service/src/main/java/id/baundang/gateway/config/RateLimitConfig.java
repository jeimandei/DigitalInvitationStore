package id.baundang.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Configuration
public class RateLimitConfig {

    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String ip = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
            if (ip == null || ip.isBlank()) {
                ip = Objects.requireNonNull(
                        exchange.getRequest().getRemoteAddress()
                ).getAddress().getHostAddress();
            } else {
                ip = ip.split(",")[0].trim();
            }
            return Mono.just(ip);
        };
    }
}
