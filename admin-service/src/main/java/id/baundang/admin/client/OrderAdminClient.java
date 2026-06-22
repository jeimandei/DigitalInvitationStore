package id.baundang.admin.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.baundang.admin.dto.OrderDTO;
import id.baundang.admin.dto.OrderRevisionDTO;
import id.baundang.admin.dto.PagedResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderAdminClient {

    @Qualifier("orderRestClient")
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public PagedResult<OrderDTO> listOrders(int page, int size, String status, String search) {
        try {
            StringBuilder uri = new StringBuilder("/api/v1/orders?page=").append(page)
                    .append("&size=").append(size);
            if (status != null && !status.isBlank()) uri.append("&status=").append(status);
            if (search != null && !search.isBlank()) uri.append("&search=").append(search);

            JsonNode root = restClient.get().uri(uri.toString())
                    .retrieve().body(JsonNode.class);
            JsonNode data = root.path("data");
            List<OrderDTO> items = objectMapper.convertValue(
                    data.path("content"),
                    new TypeReference<>() {}
            );
            return new PagedResult<>(
                    items,
                    data.path("page").asInt(0),
                    data.path("size").asInt(size),
                    data.path("totalElements").asLong(0),
                    data.path("totalPages").asInt(0),
                    data.path("last").asBoolean(true)
            );
        } catch (RestClientException e) {
            log.error("Failed to list orders: {}", e.getMessage());
            return PagedResult.empty();
        }
    }

    public Optional<OrderDTO> getOrder(UUID id) {
        try {
            JsonNode root = restClient.get()
                    .uri("/api/v1/orders/" + id)
                    .header("X-User-Role", "ADMIN")
                    .header("X-User-Id", "admin")
                    .retrieve().body(JsonNode.class);
            return Optional.of(objectMapper.convertValue(root.path("data"), OrderDTO.class));
        } catch (RestClientException e) {
            log.error("Failed to get order {}: {}", id, e.getMessage());
            return Optional.empty();
        }
    }

    public List<OrderRevisionDTO> getRevisions(UUID orderId) {
        try {
            JsonNode root = restClient.get()
                    .uri("/api/v1/orders/" + orderId + "/revisions")
                    .header("X-User-Role", "ADMIN")
                    .header("X-User-Id", "admin")
                    .retrieve().body(JsonNode.class);
            return objectMapper.convertValue(root.path("data"), new TypeReference<>() {});
        } catch (RestClientException e) {
            log.error("Failed to get revisions for {}: {}", orderId, e.getMessage());
            return List.of();
        }
    }

    public Optional<OrderRevisionDTO> completeRevision(UUID revisionId) {
        try {
            JsonNode root = restClient.put()
                    .uri("/api/v1/orders/revisions/" + revisionId + "/complete")
                    .header("X-User-Role", "ADMIN")
                    .header("X-User-Id", "admin")
                    .retrieve().body(JsonNode.class);
            return Optional.of(objectMapper.convertValue(root.path("data"), OrderRevisionDTO.class));
        } catch (RestClientException e) {
            log.error("Failed to complete revision {}: {}", revisionId, e.getMessage());
            return Optional.empty();
        }
    }

    public boolean updateStatus(UUID id, String status, String midtransId) {
        try {
            Map<String, Object> body = midtransId != null
                    ? Map.of("status", status, "midtransTransactionId", midtransId)
                    : Map.of("status", status);
            restClient.put()
                    .uri("/api/v1/orders/" + id + "/status")
                    .header("X-User-Role", "ADMIN")
                    .header("X-User-Id", "admin")
                    .body(body)
                    .retrieve().toBodilessEntity();
            return true;
        } catch (RestClientException e) {
            log.error("Failed to update order status {}: {}", id, e.getMessage());
            return false;
        }
    }
}
