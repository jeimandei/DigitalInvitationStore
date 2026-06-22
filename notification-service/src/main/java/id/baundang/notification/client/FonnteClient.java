package id.baundang.notification.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@Component
public class FonnteClient {

    private final RestClient restClient;

    public FonnteClient(
            @Value("${app.fonnte.token}") String token,
            @Value("${app.fonnte.url}") String url) {
        this.restClient = RestClient.builder()
                .baseUrl(url)
                .defaultHeader("Authorization", token)
                .build();
    }

    public void send(String target, String message) {
        try {
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
            log.info("WA sent to {}", target);
        } catch (Exception e) {
            log.error("Failed to send WA to {}: {}", target, e.getMessage());
            throw e;
        }
    }
}
