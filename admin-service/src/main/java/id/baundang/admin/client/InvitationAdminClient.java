package id.baundang.admin.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.baundang.admin.dto.AttendanceDTO;
import id.baundang.admin.dto.GiftAccountDTO;
import id.baundang.admin.dto.GiftSummaryDTO;
import id.baundang.admin.dto.GuestDTO;
import id.baundang.admin.dto.GuestbookEntryDTO;
import id.baundang.admin.dto.InvitationDTO;
import id.baundang.admin.dto.PagedResult;
import id.baundang.admin.dto.RsvpEntryDTO;
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
                    .header("X-User-Id", "00000000-0000-0000-0000-000000000001")
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

    public InvitationDTO getInvitation(UUID id) {
        try {
            JsonNode root = restClient.get()
                    .uri("/api/v1/admin/invitations/" + id)
                    .header("X-User-Role", "ADMIN")
                    .header("X-User-Id", "00000000-0000-0000-0000-000000000001")
                    .retrieve().body(JsonNode.class);
            return objectMapper.convertValue(root.path("data"), InvitationDTO.class);
        } catch (RestClientException e) {
            log.error("Failed to get invitation {}: {}", id, e.getMessage());
            return null;
        }
    }

    public List<GuestbookEntryDTO> listAllGuestbook(UUID invitationId) {
        try {
            JsonNode root = restClient.get()
                    .uri("/api/v1/admin/invitations/" + invitationId + "/guestbook")
                    .header("X-User-Role", "ADMIN")
                    .header("X-User-Id", "00000000-0000-0000-0000-000000000001")
                    .retrieve().body(JsonNode.class);
            return objectMapper.convertValue(root.path("data"), new TypeReference<>() {});
        } catch (RestClientException e) {
            log.error("Failed to list guestbook {}: {}", invitationId, e.getMessage());
            return List.of();
        }
    }

    public boolean updateContent(UUID invitationId, Object contentPatch) {
        try {
            restClient.put()
                    .uri("/api/v1/admin/invitations/" + invitationId + "/content")
                    .header("X-User-Role", "ADMIN")
                    .header("X-User-Id", "00000000-0000-0000-0000-000000000001")
                    .body(contentPatch)
                    .retrieve().toBodilessEntity();
            return true;
        } catch (RestClientException e) {
            log.error("Failed to update invitation content {}: {}", invitationId, e.getMessage());
            return false;
        }
    }

    public boolean approveGuestbook(UUID invitationId, UUID entryId) {
        try {
            restClient.put()
                    .uri("/api/v1/admin/invitations/" + invitationId + "/approve-guestbook/" + entryId)
                    .header("X-User-Role", "ADMIN")
                    .header("X-User-Id", "00000000-0000-0000-0000-000000000001")
                    .retrieve().toBodilessEntity();
            return true;
        } catch (RestClientException e) {
            log.error("Failed to approve guestbook entry {}: {}", entryId, e.getMessage());
            return false;
        }
    }

    public List<RsvpEntryDTO> listRsvp(UUID invitationId) {
        try {
            JsonNode root = restClient.get()
                    .uri("/api/v1/admin/invitations/" + invitationId + "/rsvp")
                    .header("X-User-Role", "ADMIN")
                    .header("X-User-Id", "00000000-0000-0000-0000-000000000001")
                    .retrieve().body(JsonNode.class);
            return objectMapper.convertValue(root.path("data"), new TypeReference<>() {});
        } catch (RestClientException e) {
            log.error("Failed to list rsvp {}: {}", invitationId, e.getMessage());
            return List.of();
        }
    }

    public List<GuestDTO> listGuests(UUID invitationId) {
        try {
            JsonNode root = restClient.get()
                    .uri("/api/v1/admin/invitations/" + invitationId + "/guests")
                    .header("X-User-Role", "ADMIN")
                    .header("X-User-Id", "00000000-0000-0000-0000-000000000001")
                    .retrieve().body(JsonNode.class);
            return objectMapper.convertValue(root.path("data"), new TypeReference<>() {});
        } catch (RestClientException e) {
            log.error("Failed to list guests {}: {}", invitationId, e.getMessage());
            return List.of();
        }
    }

    public boolean addGuest(UUID invitationId, Map<String, Object> req) {
        try {
            restClient.post()
                    .uri("/api/v1/admin/invitations/" + invitationId + "/guests")
                    .header("X-User-Role", "ADMIN")
                    .header("X-User-Id", "00000000-0000-0000-0000-000000000001")
                    .body(req)
                    .retrieve().toBodilessEntity();
            return true;
        } catch (RestClientException e) {
            log.error("Failed to add guest to {}: {}", invitationId, e.getMessage());
            return false;
        }
    }

    public boolean deleteGuest(UUID invitationId, UUID guestId) {
        try {
            restClient.delete()
                    .uri("/api/v1/admin/invitations/" + invitationId + "/guests/" + guestId)
                    .header("X-User-Role", "ADMIN")
                    .header("X-User-Id", "00000000-0000-0000-0000-000000000001")
                    .retrieve().toBodilessEntity();
            return true;
        } catch (RestClientException e) {
            log.error("Failed to delete guest {} from {}: {}", guestId, invitationId, e.getMessage());
            return false;
        }
    }

    public AttendanceDTO getAttendance(UUID invitationId) {
        try {
            JsonNode root = restClient.get()
                    .uri("/api/v1/admin/invitations/" + invitationId + "/attendance")
                    .header("X-User-Role", "ADMIN")
                    .header("X-User-Id", "00000000-0000-0000-0000-000000000001")
                    .retrieve().body(JsonNode.class);
            return objectMapper.convertValue(root.path("data"), AttendanceDTO.class);
        } catch (RestClientException e) {
            log.error("Failed to get attendance {}: {}", invitationId, e.getMessage());
            return null;
        }
    }

    public GiftSummaryDTO getGifts(UUID invitationId) {
        try {
            JsonNode root = restClient.get()
                    .uri("/api/v1/admin/invitations/" + invitationId + "/gifts")
                    .header("X-User-Role", "ADMIN")
                    .header("X-User-Id", "00000000-0000-0000-0000-000000000001")
                    .retrieve().body(JsonNode.class);
            return objectMapper.convertValue(root.path("data"), GiftSummaryDTO.class);
        } catch (RestClientException e) {
            log.error("Failed to get gifts {}: {}", invitationId, e.getMessage());
            return null;
        }
    }

    public GiftAccountDTO getGiftAccount(UUID invitationId) {
        try {
            JsonNode root = restClient.get()
                    .uri("/api/v1/admin/invitations/" + invitationId + "/gift-accounts")
                    .header("X-User-Role", "ADMIN")
                    .header("X-User-Id", "00000000-0000-0000-0000-000000000001")
                    .retrieve().body(JsonNode.class);
            return objectMapper.convertValue(root.path("data"), GiftAccountDTO.class);
        } catch (RestClientException e) {
            log.error("Failed to get gift account {}: {}", invitationId, e.getMessage());
            return null;
        }
    }

    public boolean setGiftAccount(UUID invitationId, GiftAccountDTO req) {
        try {
            restClient.put()
                    .uri("/api/v1/admin/invitations/" + invitationId + "/gift-accounts")
                    .header("X-User-Role", "ADMIN")
                    .header("X-User-Id", "00000000-0000-0000-0000-000000000001")
                    .body(req)
                    .retrieve().toBodilessEntity();
            return true;
        } catch (RestClientException e) {
            log.error("Failed to set gift account {}: {}", invitationId, e.getMessage());
            return false;
        }
    }

    public boolean updateStatus(UUID invitationId, String status) {
        try {
            restClient.put()
                    .uri("/api/v1/admin/invitations/" + invitationId + "/status")
                    .header("X-User-Role", "ADMIN")
                    .header("X-User-Id", "00000000-0000-0000-0000-000000000001")
                    .body(Map.of("status", status))
                    .retrieve().toBodilessEntity();
            return true;
        } catch (RestClientException e) {
            log.error("Failed to update status {}: {}", invitationId, e.getMessage());
            return false;
        }
    }
}
