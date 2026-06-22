package id.baundang.invitation.controller;

import com.fasterxml.jackson.databind.JsonNode;
import id.baundang.common.dto.ApiResponse;
import id.baundang.invitation.domain.Invitation.InvitationStatus;
import id.baundang.invitation.dto.*;
import jakarta.validation.Valid;
import id.baundang.invitation.service.InvitationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    // Internal — called by notification-service scheduler (no JWT, internal network only)
    @GetMapping("/api/v1/invitations/expiring")
    public List<ExpiringInvitationDTO> listExpiring(@RequestParam(defaultValue = "7") int days) {
        return invitationService.findExpiring(days);
    }

    // --- Admin ---

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
}
