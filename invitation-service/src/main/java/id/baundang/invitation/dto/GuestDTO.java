package id.baundang.invitation.dto;

import id.baundang.invitation.domain.Guest;

import java.time.Instant;
import java.util.UUID;

public record GuestDTO(
        UUID id,
        String name,
        String inviteCode,
        String groupLabel,
        String tableNo,
        short allottedCount,
        boolean checkedIn,
        Instant checkedInAt,
        short checkedInCount
) {
    public static GuestDTO from(Guest g) {
        return new GuestDTO(
                g.getId(), g.getName(), g.getInviteCode(),
                g.getGroupLabel(), g.getTableNo(), g.getAllottedCount(),
                g.isCheckedIn(), g.getCheckedInAt(), g.getCheckedInCount()
        );
    }
}
