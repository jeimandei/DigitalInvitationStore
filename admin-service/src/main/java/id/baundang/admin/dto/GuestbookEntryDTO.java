package id.baundang.admin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GuestbookEntryDTO(
        UUID id,
        String guestName,
        String message,
        boolean approved,
        Instant createdAt
) {}
