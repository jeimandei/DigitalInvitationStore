package id.baundang.admin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GiftEntryDTO(
        UUID id,
        String senderName,
        long amount,
        String message,
        Instant createdAt
) {}
