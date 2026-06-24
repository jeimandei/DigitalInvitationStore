package id.baundang.admin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RsvpEntryDTO(
        UUID id,
        String guestName,
        String phone,
        String attendance,
        short guestCount,
        String message,
        Instant submittedAt
) {}
