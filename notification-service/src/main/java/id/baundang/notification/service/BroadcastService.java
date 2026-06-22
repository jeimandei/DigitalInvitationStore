package id.baundang.notification.service;

import id.baundang.notification.client.FonnteClient;
import id.baundang.notification.dto.BroadcastRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BroadcastService {

    private final FonnteClient fonnteClient;

    @Value("${app.services.invitation-service}")
    private String invitationServiceUrl;

    public void broadcast(BroadcastRequest req) {
        List<String> phones = fetchPhones(req.targetGroup());
        if (phones.isEmpty()) {
            log.warn("Broadcast to {} returned no phone numbers", req.targetGroup());
            return;
        }
        log.info("Broadcasting to {} phones (group={})", phones.size(), req.targetGroup());
        fonnteClient.sendBulk(phones, req.message());
    }

    private List<String> fetchPhones(String targetGroup) {
        RestClient client = RestClient.builder().baseUrl(invitationServiceUrl).build();
        return switch (targetGroup) {
            case "EXPIRING_7D" -> {
                List<ExpiringPhone> expiring = client.get()
                        .uri("/api/v1/invitations/expiring?days=7")
                        .retrieve()
                        .body(new ParameterizedTypeReference<List<ExpiringPhone>>() {});
                yield expiring == null ? List.of()
                        : expiring.stream()
                                  .map(ExpiringPhone::coupleWhatsapp)
                                  .filter(wa -> wa != null && !wa.isBlank())
                                  .distinct()
                                  .toList();
            }
            default -> { // ALL_ACTIVE
                List<String> phones = client.get()
                        .uri("/api/v1/admin/invitations/active-phones")
                        .retrieve()
                        .body(new ParameterizedTypeReference<List<String>>() {});
                yield phones != null ? phones : List.of();
            }
        };
    }

    private record ExpiringPhone(String coupleWhatsapp) {}
}
