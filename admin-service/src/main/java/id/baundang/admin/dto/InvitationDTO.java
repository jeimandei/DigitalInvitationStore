package id.baundang.admin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InvitationDTO(
        UUID id,
        UUID orderId,
        String coupleSlug,
        UUID templateId,
        JsonNode content,
        String status,
        LocalDate activeUntil,
        long viewCount
) {}
