package id.baundang.storefront.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

@Component
public class OrderApiClient {

    private static final Logger LOG = LoggerFactory.getLogger(OrderApiClient.class);

    private final RestClient restClient;

    public OrderApiClient(RestClient orderRestClient) {
        this.restClient = orderRestClient;
    }

    public PublicOrderDTO fetchPublicOrder(UUID orderId) {
        try {
            JsonNode body = restClient.get()
                    .uri("/api/v1/orders/public/{id}", orderId)
                    .retrieve()
                    .body(JsonNode.class);

            if (body == null) return null;

            JsonNode data = body.has("data") ? body.get("data") : body;
            return new PublicOrderDTO(
                    data.path("id").asText(),
                    data.path("orderNumber").asText(),
                    data.path("tier").asInt(),
                    data.path("amount").asLong(),
                    data.path("coupleName").asText(),
                    data.path("status").asText(),
                    data.path("revisionCount").asInt(),
                    data.path("maxRevisions").asInt(),
                    data.path("coupleSlug").asText(null)
            );
        } catch (RestClientException e) {
            LOG.warn("Could not fetch public order {}: {}", orderId, e.getMessage());
            return null;
        }
    }

    public record PublicOrderDTO(
            String id,
            String orderNumber,
            int tier,
            long amount,
            String coupleName,
            String status,
            int revisionCount,
            int maxRevisions,
            String coupleSlug
    ) {}
}
