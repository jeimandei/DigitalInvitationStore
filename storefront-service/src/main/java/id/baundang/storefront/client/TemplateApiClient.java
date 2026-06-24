package id.baundang.storefront.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Collections;
import java.util.List;

@Component
public class TemplateApiClient {

    private static final Logger LOG = LoggerFactory.getLogger(TemplateApiClient.class);

    private final RestClient restClient;

    public TemplateApiClient(RestClient templateRestClient) {
        this.restClient = templateRestClient;
    }

    public TemplatePage fetchTemplates(int page, int size, String category) {
        try {
            String uri = buildUri(page, size, category);
            JsonNode body = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(JsonNode.class);

            if (body == null) {
                return TemplatePage.empty();
            }

            JsonNode data = body.has("data") ? body.get("data") : body;

            List<TemplateSummaryDTO> content = parseContent(data.get("content"));
            int totalPages     = data.path("totalPages").asInt(1);
            long totalElements = data.path("totalElements").asLong(0);
            boolean last       = data.path("last").asBoolean(true);

            return new TemplatePage(content, page, size, totalElements, totalPages, last);
        } catch (RestClientException e) {
            LOG.warn("Template service unavailable, returning empty page: {}", e.getMessage());
            return TemplatePage.empty();
        }
    }

    private String buildUri(int page, int size, String category) {
        StringBuilder sb = new StringBuilder("/api/v1/templates?page=")
                .append(page)
                .append("&size=").append(size);
        if (category != null && !category.isBlank()) {
            sb.append("&category=").append(category);
        }
        return sb.toString();
    }

    private List<TemplateSummaryDTO> parseContent(JsonNode contentNode) {
        if (contentNode == null || !contentNode.isArray()) {
            return Collections.emptyList();
        }

        return contentNode.findValues("id").isEmpty()
                ? Collections.emptyList()
                : java.util.stream.StreamSupport.stream(contentNode.spliterator(), false)
                        .map(n -> new TemplateSummaryDTO(
                                n.path("id").asText(),
                                n.path("name").asText(),
                                n.path("slug").asText(),
                                n.path("thumbnailUrl").asText(),
                                n.path("category").asText(),
                                n.path("priceLevel").asText()
                        ))
                        .toList();
    }

    public record TemplatePage(
            List<TemplateSummaryDTO> content,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean last
    ) {
        static TemplatePage empty() {
            return new TemplatePage(Collections.emptyList(), 0, 12, 0, 0, true);
        }
    }
}
