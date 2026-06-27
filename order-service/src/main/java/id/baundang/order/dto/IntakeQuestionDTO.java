package id.baundang.order.dto;

import com.fasterxml.jackson.databind.JsonNode;
import id.baundang.order.domain.IntakeQuestion;

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
) {
    public static IntakeQuestionDTO from(IntakeQuestion q) {
        return new IntakeQuestionDTO(
                q.getId(), q.getSection(), q.getLabel(), q.getFieldKey(),
                q.getInputType(), q.getOptions(), q.getMinTier(), q.getRequired(),
                q.getSortOrder(), q.getActive()
        );
    }
}
