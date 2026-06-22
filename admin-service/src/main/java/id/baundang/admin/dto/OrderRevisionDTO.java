package id.baundang.admin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderRevisionDTO(
        UUID id,
        UUID orderId,
        UUID requestedBy,
        JsonNode changes,
        String status,
        Instant createdAt
) {}
