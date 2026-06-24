package id.baundang.invitation.dto;

public record AttendanceDTO(
        long totalInvited,
        long totalAllotted,
        long checkedInGuests,
        long checkedInCount,
        int percentage
) {
    public static AttendanceDTO of(long totalInvited, long totalAllotted,
                                   long checkedInGuests, long checkedInCount) {
        int pct = totalAllotted == 0 ? 0
                : (int) Math.round(checkedInCount * 100.0 / totalAllotted);
        return new AttendanceDTO(totalInvited, totalAllotted,
                checkedInGuests, checkedInCount, pct);
    }
}
