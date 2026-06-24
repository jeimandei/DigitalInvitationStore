package id.baundang.invitation.dto;

import id.baundang.invitation.domain.RsvpResponse;

import java.time.Instant;
import java.util.UUID;

public record RsvpResponseDTO(
        UUID id,
        String guestName,
        String phone,
        String attendance,
        short guestCount,
        String message,
        Instant submittedAt
) {
    public static RsvpResponseDTO from(RsvpResponse r) {
        return new RsvpResponseDTO(
                r.getId(),
                r.getGuestName(),
                r.getPhone(),
                r.getAttendance(),
                r.getGuestCount() != null ? r.getGuestCount() : 1,
                r.getMessage(),
                r.getSubmittedAt()
        );
    }
}
