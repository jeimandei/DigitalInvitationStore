package id.baundang.notification.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Polls invitation-service daily at 08:00 WIB (UTC+7 = 01:00 UTC) for invitations
 * expiring within 7 days and publishes invitation.expiring events so the consumer
 * in this same service can dispatch WhatsApp reminders.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InvitationExpiryScheduler {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.invitations-exchange}")
    private String invitationsExchange;

    // Lazy RestClient — invitation-service URL from config
    @Value("${app.invitation-service.url:http://invitation-service:8084}")
    private String invitationServiceUrl;

    @Scheduled(cron = "0 0 1 * * *", zone = "UTC") // 08:00 WIB = 01:00 UTC
    public void checkExpiringInvitations() {
        log.info("Running expiring-invitation check");
        try {
            RestClient client = RestClient.builder().baseUrl(invitationServiceUrl).build();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> expiring = client.get()
                    .uri("/api/v1/invitations/expiring?days=7")
                    .retrieve()
                    .body(List.class);

            if (expiring == null || expiring.isEmpty()) {
                log.info("No expiring invitations found");
                return;
            }

            for (Map<String, Object> inv : expiring) {
                try {
                    rabbitTemplate.convertAndSend(
                            invitationsExchange,
                            "invitation.expiring",
                            inv
                    );
                } catch (Exception e) {
                    log.error("Failed to publish invitation.expiring for {}: {}",
                            inv.get("invitationId"), e.getMessage());
                }
            }
            log.info("Published {} invitation.expiring events", expiring.size());
        } catch (Exception e) {
            log.error("Expiring-invitation check failed: {}", e.getMessage(), e);
        }
    }
}
