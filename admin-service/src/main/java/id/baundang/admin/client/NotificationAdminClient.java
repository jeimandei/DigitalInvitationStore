package id.baundang.admin.client;

import id.baundang.admin.dto.BroadcastRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class NotificationAdminClient {

    private final RestClient restClient;

    public NotificationAdminClient(@Qualifier("notificationRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public void broadcast(BroadcastRequest req) {
        restClient.post()
                .uri("/api/v1/notifications/broadcast")
                .contentType(MediaType.APPLICATION_JSON)
                .body(req)
                .retrieve()
                .toBodilessEntity();
        log.info("Broadcast triggered: group={}", req.targetGroup());
    }
}
