package id.baundang.invitation.dto;

import id.baundang.invitation.domain.GuestbookEntry;

import java.time.Instant;
import java.util.UUID;

public record GuestbookEntryDTO(
        UUID id,
        String guestName,
        String message,
        Instant createdAt
) {
    public static GuestbookEntryDTO from(GuestbookEntry e) {
        return new GuestbookEntryDTO(e.getId(), e.getGuestName(), e.getMessage(), e.getCreatedAt());
    }
}
