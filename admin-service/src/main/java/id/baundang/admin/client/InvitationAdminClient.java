package id.baundang.admin.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.baundang.admin.dto.GuestbookEntryDTO;
import id.baundang.admin.dto.InvitationDTO;
import id.baundang.admin.dto.PagedResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvitationAdminClient {

    @Qualifier("invitationRestClient")
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public PagedResult<InvitationDTO> listInvitations(int page, int size) {
        try {
            JsonNode root = restClient.get()
                    .uri("/api/v1/admin/invitations?page=" + page + "&size=" + size)
                    .header("X-User-Role", "ADMIN")
                    .header("X-User-Id", "admin")
                    .retrieve().body(JsonNode.class);
            JsonNode data = root.path("data");
            List<InvitationDTO> items = objectMapper.convertValue(
                    data.path("content"), new TypeReference<>() {}
            );
            return new PagedResult<>(items,
                    data.path("page").asInt(0), data.path("size").asInt(size),
                    data.path("totalElements").asLong(0),
                    data.path("totalPages").asInt(0), data.path("last").asBoolean(true));
        } catch (RestClientException e) {
            log.error("Failed to list invitations: {}", e.getMessage());
            return PagedResult.empty();
        }
    }

    public List<GuestbookEntryDTO> listAllGuestbook(UUID invitationId) {
        try {
            JsonNode root = restClient.get()
                    .uri("/api/v1/admin/invitations/" + invitationId + "/guestbook")
                    .header("X-User-Role", "ADMIN")
                    .header("X-User-Id", "admin")
                    .retrieve().body(JsonNode.class);
            return objectMapper.convertValue(root.path("data"), new TypeReference<>() {});
        } catch (RestClientException e) {
            log.error("Failed to list guestbook {}: {}", invitationId, e.getMessage());
            return List.of();
        }
    }

    public boolean approveGuestbook(UUID invitationId, UUID entryId) {
        try {
            restClient.put()
                    .uri("/api/v1/admin/invitations/" + invitationId + "/approve-guestbook/" + entryId)
                    .header("X-User-Role", "ADMIN")
                    .header("X-User-Id", "admin")
                    .retrieve().toBodilessEntity();
            return true;
        } catch (RestClientException e) {
            log.error("Failed to approve guestbook entry {}: {}", entryId, e.getMessage());
            return false;
        }
    }

    public List<Map<String, Object>> listRsvp(UUID invitationId) {
        try {
            JsonNode root = restClient.get()
                    .uri("/api/v1/admin/invitations/" + invitationId + "/rsvp")
                    .header("X-User-Role", "ADMIN")
                    .header("X-User-Id", "admin")
                    .retrieve().body(JsonNode.class);
            return objectMapper.convertValue(root.path("data"), new TypeReference<>() {});
        } catch (RestClientException e) {
            log.error("Failed to list rsvp {}: {}", invitationId, e.getMessage());
            return List.of();
        }
    }
}
