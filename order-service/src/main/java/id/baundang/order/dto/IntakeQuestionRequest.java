package id.baundang.order.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;

public record IntakeQuestionRequest(
        String section,
        @NotBlank String label,
        @NotBlank String fieldKey,
        String inputType,
        JsonNode options,
        Short minTier,
        Boolean required,
        Integer sortOrder,
        Boolean active
) {}
