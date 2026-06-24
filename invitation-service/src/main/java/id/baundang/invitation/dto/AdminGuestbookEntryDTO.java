package id.baundang.invitation.dto;

import id.baundang.invitation.domain.GuestbookEntry;

import java.time.Instant;
import java.util.UUID;

public record AdminGuestbookEntryDTO(
        UUID id,
        String guestName,
        String message,
        boolean approved,
        Instant createdAt
) {
    public static AdminGuestbookEntryDTO from(GuestbookEntry e) {
        return new AdminGuestbookEntryDTO(
                e.getId(),
                e.getGuestName(),
                e.getMessage(),
                Boolean.TRUE.equals(e.getApproved()),
                e.getCreatedAt()
        );
    }
}
