package id.baundang.admin.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.baundang.admin.dto.PagedResult;
import id.baundang.admin.dto.TemplateCreateRequest;
import id.baundang.admin.dto.TemplateDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class TemplateAdminClient {

    @Qualifier("templateRestClient")
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public PagedResult<TemplateDTO> listTemplates(int page, int size) {
        try {
            JsonNode root = restClient.get()
                    .uri("/api/v1/templates?page=" + page + "&size=" + size + "&includeInactive=true")
                    .header("X-User-Role", "ADMIN")
                    .header("X-User-Id", "admin")
                    .retrieve().body(JsonNode.class);
            JsonNode data = root.path("data");
            List<TemplateDTO> items = objectMapper.convertValue(
                    data.path("content"), new TypeReference<>() {}
            );
            return new PagedResult<>(items,
                    data.path("page").asInt(0), data.path("size").asInt(size),
                    data.path("totalElements").asLong(0),
                    data.path("totalPages").asInt(0), data.path("last").asBoolean(true));
        } catch (RestClientException e) {
            log.error("Failed to list templates: {}", e.getMessage());
            return PagedResult.empty();
        }
    }

    public TemplateDTO getTemplate(String slug) {
        try {
            JsonNode root = restClient.get()
                    .uri("/api/v1/templates/" + slug)
                    .header("X-User-Role", "ADMIN")
                    .header("X-User-Id", "admin")
                    .retrieve().body(JsonNode.class);
            return objectMapper.convertValue(root.path("data"), TemplateDTO.class);
        } catch (RestClientException e) {
            log.error("Failed to get template {}: {}", slug, e.getMessage());
            return null;
        }
    }

    public boolean toggleActive(String id, boolean active) {
        try {
            restClient.put()
                    .uri("/api/v1/templates/" + id + "/active?active=" + active)
                    .header("X-User-Role", "ADMIN")
                    .header("X-User-Id", "admin")
                    .retrieve().toBodilessEntity();
            return true;
        } catch (RestClientException e) {
            log.error("Failed to toggle template {}: {}", id, e.getMessage());
            return false;
        }
    }

    public boolean createTemplate(TemplateCreateRequest req) {
        try {
            restClient.post()
                    .uri("/api/v1/templates")
                    .header("X-User-Role", "ADMIN")
                    .header("X-User-Id", "admin")
                    .body(req)
                    .retrieve().toBodilessEntity();
            return true;
        } catch (RestClientException e) {
            log.error("Failed to create template: {}", e.getMessage());
            return false;
        }
    }

    public boolean updateTemplate(UUID id, TemplateCreateRequest req) {
        try {
            restClient.put()
                    .uri("/api/v1/templates/" + id)
                    .header("X-User-Role", "ADMIN")
                    .header("X-User-Id", "admin")
                    .body(req)
                    .retrieve().toBodilessEntity();
            return true;
        } catch (RestClientException e) {
            log.error("Failed to update template {}: {}", id, e.getMessage());
            return false;
        }
    }

    public boolean deleteTemplate(UUID id) {
        try {
            restClient.delete()
                    .uri("/api/v1/templates/" + id)
                    .header("X-User-Role", "ADMIN")
                    .header("X-User-Id", "admin")
                    .retrieve().toBodilessEntity();
            return true;
        } catch (RestClientException e) {
            log.error("Failed to delete template {}: {}", id, e.getMessage());
            return false;
        }
    }
}
