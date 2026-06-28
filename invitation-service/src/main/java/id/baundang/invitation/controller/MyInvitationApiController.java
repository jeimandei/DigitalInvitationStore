package id.baundang.invitation.controller;

import com.fasterxml.jackson.databind.JsonNode;
import id.baundang.common.ApiResponse;
import id.baundang.common.exception.NotFoundException;
import id.baundang.common.exception.UnauthorizedException;
import id.baundang.invitation.domain.Invitation;
import id.baundang.invitation.dto.AdminGuestbookEntryDTO;
import id.baundang.invitation.dto.AttendanceDTO;
import id.baundang.invitation.dto.GiftSummaryDTO;
import id.baundang.invitation.dto.GuestDTO;
import id.baundang.invitation.dto.GuestRequest;
import id.baundang.invitation.dto.RsvpResponseDTO;
import id.baundang.invitation.repository.InvitationRepository;
import id.baundang.invitation.service.InvitationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

/**
 * Client (buyer) self-service endpoints, scoped by orderId and gated on
 * ownership: the authenticated user's id (X-User-Id, injected by the gateway's
 * JwtAuth filter) must match the buyerId stored in the invitation content.
 *
 * Delegates to the same InvitationService methods the admin UI uses.
 */
@RestController
@RequestMapping("/api/v1/invitations/my")
@RequiredArgsConstructor
public class MyInvitationApiController {

    private final InvitationService invitationService;
    private final InvitationRepository invitationRepository;

    // ── Guests ────────────────────────────────────────────────────────────────

    @GetMapping("/{orderId}/guests")
    public ApiResponse<List<GuestDTO>> listGuests(@PathVariable UUID orderId, Principal principal) {
        Invitation inv = requireOwned(orderId, principal);
        return ApiResponse.ok(invitationService.listGuests(inv.getId()));
    }

    @PostMapping("/{orderId}/guests")
    public ApiResponse<GuestDTO> addGuest(@PathVariable UUID orderId,
                                          @Valid @RequestBody GuestRequest req,
                                          Principal principal) {
        Invitation inv = requireOwned(orderId, principal);
        return ApiResponse.ok(invitationService.addGuest(inv.getId(), req), "Tamu berhasil ditambahkan");
    }

    @DeleteMapping("/{orderId}/guests/{guestId}")
    public ApiResponse<Void> removeGuest(@PathVariable UUID orderId,
                                         @PathVariable UUID guestId,
                                         Principal principal) {
        Invitation inv = requireOwned(orderId, principal);
        invitationService.removeGuest(inv.getId(), guestId);
        return ApiResponse.ok(null, "Tamu dihapus");
    }

    // ── RSVP ──────────────────────────────────────────────────────────────────

    @GetMapping("/{orderId}/rsvp")
    public ApiResponse<List<RsvpResponseDTO>> listRsvp(@PathVariable UUID orderId, Principal principal) {
        Invitation inv = requireOwned(orderId, principal);
        return ApiResponse.ok(invitationService.listRsvp(inv.getId()));
    }

    // ── Attendance / check-in stats ───────────────────────────────────────────

    @GetMapping("/{orderId}/attendance")
    public ApiResponse<AttendanceDTO> getAttendance(@PathVariable UUID orderId, Principal principal) {
        Invitation inv = requireOwned(orderId, principal);
        return ApiResponse.ok(invitationService.getAttendance(inv.getId()));
    }

    // ── Digital gifts (amplop) summary ────────────────────────────────────────

    @GetMapping("/{orderId}/gifts")
    public ApiResponse<GiftSummaryDTO> getGifts(@PathVariable UUID orderId, Principal principal) {
        Invitation inv = requireOwned(orderId, principal);
        return ApiResponse.ok(invitationService.getGiftSummary(inv.getId()));
    }

    // ── Guestbook (view + approve) ────────────────────────────────────────────

    @GetMapping("/{orderId}/guestbook")
    public ApiResponse<List<AdminGuestbookEntryDTO>> listGuestbook(@PathVariable UUID orderId,
                                                                   Principal principal) {
        Invitation inv = requireOwned(orderId, principal);
        return ApiResponse.ok(invitationService.listAllGuestbook(inv.getId()));
    }

    @PutMapping("/{orderId}/guestbook/{entryId}/approve")
    public ApiResponse<Void> approveGuestbook(@PathVariable UUID orderId,
                                              @PathVariable UUID entryId,
                                              Principal principal) {
        Invitation inv = requireOwned(orderId, principal);
        invitationService.approveGuestbook(inv.getId(), entryId);
        return ApiResponse.ok(null, "Ucapan disetujui");
    }

    // ── Invitation summary (slug + couple name for the portal header) ─────────

    @GetMapping("/{orderId}")
    public ApiResponse<MyInvitationDTO> getMyInvitation(@PathVariable UUID orderId, Principal principal) {
        Invitation inv = requireOwned(orderId, principal);
        JsonNode c = inv.getContent();
        String coupleName = c != null && c.hasNonNull("coupleName")
                ? c.get("coupleName").asText("") : "";
        return ApiResponse.ok(new MyInvitationDTO(
                inv.getId(), inv.getCoupleSlug(), coupleName, inv.getStatus().name()));
    }

    public record MyInvitationDTO(UUID id, String coupleSlug, String coupleName, String status) {}

    // ── Ownership guard ───────────────────────────────────────────────────────

    private Invitation requireOwned(UUID orderId, Principal principal) {
        Invitation inv = invitationRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NotFoundException("Undangan tidak ditemukan"));
        if (principal == null) {
            throw new UnauthorizedException("Akses ditolak");
        }
        JsonNode content = inv.getContent();
        String ownerId = content != null && content.hasNonNull("buyerId")
                ? content.get("buyerId").asText("") : "";
        if (ownerId.isBlank() || !ownerId.equals(principal.getName())) {
            throw new UnauthorizedException("Akses ditolak");
        }
        return inv;
    }
}
