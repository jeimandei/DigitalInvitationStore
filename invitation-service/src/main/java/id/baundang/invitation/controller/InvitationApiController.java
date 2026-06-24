package id.baundang.invitation.controller;

import com.fasterxml.jackson.databind.JsonNode;
import id.baundang.common.ApiResponse;
import id.baundang.common.PagedResponse;
import id.baundang.invitation.domain.Invitation.InvitationStatus;
import id.baundang.invitation.dto.AdminGuestbookEntryDTO;
import id.baundang.invitation.dto.AttendanceDTO;
import id.baundang.invitation.dto.CheckInRequest;
import id.baundang.invitation.dto.EventDTO;
import id.baundang.invitation.dto.ExpiringInvitationDTO;
import id.baundang.invitation.dto.GiftAccountDTO;
import id.baundang.invitation.dto.GiftAccountRequest;
import id.baundang.invitation.dto.GiftConfirmRequest;
import id.baundang.invitation.dto.GiftSummaryDTO;
import id.baundang.invitation.dto.GuestDTO;
import id.baundang.invitation.dto.GuestRequest;
import id.baundang.invitation.dto.GuestbookEntryDTO;
import id.baundang.invitation.dto.GuestbookRequest;
import id.baundang.invitation.dto.InvitationSummaryDTO;
import id.baundang.invitation.dto.RsvpRequest;
import id.baundang.invitation.dto.RsvpResponseDTO;
import id.baundang.invitation.service.InvitationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class InvitationApiController {

    private final InvitationService invitationService;

    // --- Public ---

    @PostMapping("/api/v1/invitations/{slug}/rsvp")
    public ResponseEntity<ApiResponse<Void>> submitRsvp(@PathVariable String slug,
                                                         @Valid @RequestBody RsvpRequest req) {
        invitationService.submitRsvp(slug, req);
        return ResponseEntity.ok(ApiResponse.ok(null, "Konfirmasi kehadiran berhasil dikirim"));
    }

    @GetMapping("/api/v1/invitations/{slug}/guestbook")
    public ApiResponse<List<GuestbookEntryDTO>> listGuestbook(@PathVariable String slug) {
        return ApiResponse.ok(invitationService.listApprovedGuestbook(slug));
    }

    @PostMapping("/api/v1/invitations/{slug}/guestbook")
    public ResponseEntity<ApiResponse<Void>> submitGuestbook(@PathVariable String slug,
                                                              @Valid @RequestBody GuestbookRequest req) {
        invitationService.submitGuestbook(slug, req);
        return ResponseEntity.ok(ApiResponse.ok(null, "Ucapan berhasil dikirim, menunggu persetujuan"));
    }

    @GetMapping("/api/v1/invitations/{slug}/events")
    public ApiResponse<List<EventDTO>> listEvents(@PathVariable String slug) {
        return ApiResponse.ok(invitationService.getEvents(slug));
    }

    // Internal — called by notification-service scheduler (no JWT, internal network only)
    @GetMapping("/api/v1/invitations/expiring")
    public List<ExpiringInvitationDTO> listExpiring(@RequestParam(defaultValue = "7") int days) {
        return invitationService.findExpiring(days);
    }

    // Internal — returns WhatsApp numbers for all ACTIVE invitations (used by broadcast)
    @GetMapping("/api/v1/admin/invitations/active-phones")
    public List<String> listActivePhones() {
        return invitationService.listActivePhones();
    }

    // --- Admin ---

    @GetMapping("/api/v1/admin/invitations")
    public ApiResponse<PagedResponse<InvitationSummaryDTO>> listInvitations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ApiResponse.ok(PagedResponse.from(invitationService.listInvitations(pageable)));
    }

    @GetMapping("/api/v1/admin/invitations/{id}/guestbook")
    public ApiResponse<List<AdminGuestbookEntryDTO>> listAllGuestbook(@PathVariable UUID id) {
        return ApiResponse.ok(invitationService.listAllGuestbook(id));
    }

    @GetMapping("/api/v1/admin/invitations/{id}/rsvp")
    public ApiResponse<List<RsvpResponseDTO>> listRsvp(@PathVariable UUID id) {
        return ApiResponse.ok(invitationService.listRsvp(id));
    }

    @PutMapping("/api/v1/admin/invitations/{id}/approve-guestbook/{entryId}")
    public ApiResponse<Void> approveGuestbook(@PathVariable UUID id, @PathVariable UUID entryId) {
        invitationService.approveGuestbook(id, entryId);
        return ApiResponse.ok(null, "Ucapan disetujui");
    }

    @PutMapping("/api/v1/admin/invitations/{id}/content")
    public ApiResponse<Void> updateContent(@PathVariable UUID id, @RequestBody JsonNode content) {
        invitationService.updateContent(id, content);
        return ApiResponse.ok(null, "Konten undangan diperbarui");
    }

    @PutMapping("/api/v1/admin/invitations/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable UUID id,
                                          @RequestParam InvitationStatus status) {
        invitationService.updateStatus(id, status);
        return ApiResponse.ok(null, "Status undangan diperbarui");
    }

    // --- Gift Registry ---

    @GetMapping("/api/v1/invitations/{slug}/gift-accounts")
    public ApiResponse<GiftAccountDTO> getGiftAccount(@PathVariable String slug) {
        return ApiResponse.ok(invitationService.getGiftAccount(slug));
    }

    @PostMapping("/api/v1/invitations/{slug}/gift-confirm")
    public ResponseEntity<ApiResponse<Void>> submitGiftConfirmation(
            @PathVariable String slug,
            @Valid @RequestBody GiftConfirmRequest req) {
        invitationService.submitGiftConfirmation(slug, req);
        return ResponseEntity.ok(ApiResponse.ok(null, "Konfirmasi hadiah berhasil dikirim"));
    }

    @PutMapping("/api/v1/admin/invitations/{id}/gift-accounts")
    public ApiResponse<Void> setGiftAccount(@PathVariable UUID id,
                                             @RequestBody GiftAccountRequest req) {
        invitationService.setGiftAccount(id, req);
        return ApiResponse.ok(null, "Informasi hadiah diperbarui");
    }

    // ── Guest list & check-in ─────────────────────────────────────────────────

    @PostMapping("/api/v1/admin/invitations/{id}/guests")
    public ApiResponse<GuestDTO> addGuest(@PathVariable UUID id,
                                           @Valid @RequestBody GuestRequest req) {
        return ApiResponse.ok(invitationService.addGuest(id, req), "Tamu berhasil ditambahkan");
    }

    @GetMapping("/api/v1/admin/invitations/{id}/guests")
    public ApiResponse<List<GuestDTO>> listGuests(@PathVariable UUID id) {
        return ApiResponse.ok(invitationService.listGuests(id));
    }

    @DeleteMapping("/api/v1/admin/invitations/{id}/guests/{guestId}")
    public ApiResponse<Void> removeGuest(@PathVariable UUID id, @PathVariable UUID guestId) {
        invitationService.removeGuest(id, guestId);
        return ApiResponse.ok(null, "Tamu dihapus");
    }

    @GetMapping("/api/v1/invitations/{slug}/checkin/{code}")
    public ApiResponse<GuestDTO> getGuestForCheckIn(@PathVariable String slug,
                                                     @PathVariable String code) {
        return ApiResponse.ok(invitationService.getGuestByCode(code));
    }

    @PostMapping("/api/v1/invitations/{slug}/checkin/{code}")
    public ApiResponse<GuestDTO> checkIn(@PathVariable String slug,
                                          @PathVariable String code,
                                          @Valid @RequestBody CheckInRequest req) {
        return ApiResponse.ok(invitationService.checkIn(code, req), "Check-in berhasil");
    }

    @GetMapping("/api/v1/admin/invitations/{id}/attendance")
    public ApiResponse<AttendanceDTO> getAttendance(@PathVariable UUID id) {
        return ApiResponse.ok(invitationService.getAttendance(id));
    }

    // ── Digital gifts ─────────────────────────────────────────────────────────

    @GetMapping("/api/v1/admin/invitations/{id}/gifts")
    public ApiResponse<GiftSummaryDTO> getGiftSummary(@PathVariable UUID id) {
        return ApiResponse.ok(invitationService.getGiftSummary(id));
    }
}
