package id.baundang.invitation.dto;

import id.baundang.invitation.domain.Gift;

import java.time.Instant;
import java.util.UUID;

public record GiftEntryDTO(
        UUID id,
        String senderName,
        long amount,
        String message,
        Instant createdAt
) {
    public static GiftEntryDTO from(Gift g) {
        return new GiftEntryDTO(g.getId(), g.getSenderName(),
                g.getAmount(), g.getMessage(), g.getCreatedAt());
    }
}
