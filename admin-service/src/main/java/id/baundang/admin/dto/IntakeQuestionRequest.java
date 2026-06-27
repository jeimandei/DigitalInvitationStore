package id.baundang.admin.dto;

import java.util.List;

public record IntakeQuestionRequest(
        String section,
        String label,
        String fieldKey,
        String inputType,
        List<String> options,
        Short minTier,
        Boolean required,
        Integer sortOrder,
        Boolean active
) {}
