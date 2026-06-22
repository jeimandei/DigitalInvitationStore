package id.baundang.notification.client;

import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class FonnteClient {

    private final RestClient restClient;
    @SuppressWarnings("UnstableApiUsage")
    private final RateLimiter rateLimiter = RateLimiter.create(20.0 / 60.0);

    public FonnteClient(
            @Value("${app.fonnte.token}") String token,
            @Value("${app.fonnte.url}") String url) {
        this.restClient = RestClient.builder()
                .baseUrl(url)
                .defaultHeader("Authorization", token)
                .build();
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public void send(String target, String message) {
        rateLimiter.acquire();
        try {
            doSend(target, message);
            log.info("WA sent to {}", target);
        } catch (Exception e) {
            log.error("Failed to send WA to {}: {}", target, e.getMessage());
            throw e;
        }
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public void sendBulk(List<String> targets, String message) {
        if (targets == null || targets.isEmpty()) return;
        rateLimiter.acquire();
        String joined = String.join(",", targets);
        try {
            doSend(joined, message);
            log.info("WA bulk sent to {} targets", targets.size());
        } catch (Exception e) {
            log.error("Failed to send bulk WA to {} targets: {}", targets.size(), e.getMessage());
            throw e;
        }
    }

    private void doSend(String target, String message) {
        Map<String, Object> body = Map.of(
                "target", target,
                "message", message,
                "delay", 0,
                "countryCode", "62"
        );
        restClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }
}
