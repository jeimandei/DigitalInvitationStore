package id.baundang.notification.client;

import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.List;

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
        if (targets == null || targets.isEmpty()) {
            return;
        }
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
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("target", target);
        form.add("message", message);
        String response = restClient.post()
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(form)
                .retrieve()
                .body(String.class);
        log.info("Fonnte response for target {}: {}", target, response);
    }
}
