package id.baundang.order.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;

public record RevisionRequest(
        @NotNull JsonNode changes
) {}
