package id.baundang.admin.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.UUID;

public record IntakeQuestionDTO(
        UUID id,
        String section,
        String label,
        String fieldKey,
        String inputType,
        JsonNode options,
        short minTier,
        boolean required,
        int sortOrder,
        boolean active
) {}
