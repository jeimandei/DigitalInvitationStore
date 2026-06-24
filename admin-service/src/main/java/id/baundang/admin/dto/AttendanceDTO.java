package id.baundang.admin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AttendanceDTO(
        long totalInvited,
        long totalAllotted,
        long checkedInGuests,
        long checkedInCount,
        int percentage
) {}
