package id.baundang.admin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
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
) {}
