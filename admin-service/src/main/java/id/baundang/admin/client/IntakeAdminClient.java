package id.baundang.admin.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.baundang.admin.dto.IntakeQuestionDTO;
import id.baundang.admin.dto.IntakeQuestionRequest;
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
public class IntakeAdminClient {

    private static final String ADMIN_USER_ID = "00000000-0000-0000-0000-000000000001";

    @Qualifier("orderRestClient")
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public List<IntakeQuestionDTO> listQuestions() {
        try {
            JsonNode root = restClient.get()
                    .uri("/api/v1/admin/intake/questions")
                    .header("X-User-Role", "ADMIN")
                    .header("X-User-Id", ADMIN_USER_ID)
                    .retrieve().body(JsonNode.class);
            return objectMapper.convertValue(root.path("data"), new TypeReference<>() {});
        } catch (RestClientException e) {
            log.error("Failed to list intake questions: {}", e.getMessage());
            return List.of();
        }
    }

    public boolean createQuestion(IntakeQuestionRequest req) {
        try {
            restClient.post()
                    .uri("/api/v1/admin/intake/questions")
                    .header("X-User-Role", "ADMIN")
                    .header("X-User-Id", ADMIN_USER_ID)
                    .body(req)
                    .retrieve().toBodilessEntity();
            return true;
        } catch (RestClientException e) {
            log.error("Failed to create intake question: {}", e.getMessage());
            return false;
        }
    }

    public boolean updateQuestion(UUID id, IntakeQuestionRequest req) {
        try {
            restClient.put()
                    .uri("/api/v1/admin/intake/questions/" + id)
                    .header("X-User-Role", "ADMIN")
                    .header("X-User-Id", ADMIN_USER_ID)
                    .body(req)
                    .retrieve().toBodilessEntity();
            return true;
        } catch (RestClientException e) {
            log.error("Failed to update intake question: {}", e.getMessage());
            return false;
        }
    }

    public boolean deleteQuestion(UUID id) {
        try {
            restClient.delete()
                    .uri("/api/v1/admin/intake/questions/" + id)
                    .header("X-User-Role", "ADMIN")
                    .header("X-User-Id", ADMIN_USER_ID)
                    .retrieve().toBodilessEntity();
            return true;
        } catch (RestClientException e) {
            log.error("Failed to delete intake question: {}", e.getMessage());
            return false;
        }
    }

    /** Per-order intake answers (admin viewing what the client submitted). */
    public JsonNode getOrderIntake(UUID orderId) {
        try {
            JsonNode root = restClient.get()
                    .uri("/api/v1/orders/" + orderId + "/intake")
                    .header("X-User-Role", "ADMIN")
                    .header("X-User-Id", ADMIN_USER_ID)
                    .retrieve().body(JsonNode.class);
            return root.path("data");
        } catch (RestClientException e) {
            log.error("Failed to fetch order intake {}: {}", orderId, e.getMessage());
            return null;
        }
    }

    public List<IntakeQuestionDTO> questionsForOrder(UUID orderId) {
        try {
            JsonNode root = restClient.get()
                    .uri("/api/v1/orders/" + orderId + "/intake/questions")
                    .header("X-User-Role", "ADMIN")
                    .header("X-User-Id", ADMIN_USER_ID)
                    .retrieve().body(JsonNode.class);
            return objectMapper.convertValue(root.path("data"), new TypeReference<>() {});
        } catch (RestClientException e) {
            log.error("Failed to fetch order intake questions {}: {}", orderId, e.getMessage());
            return List.of();
        }
    }
}
