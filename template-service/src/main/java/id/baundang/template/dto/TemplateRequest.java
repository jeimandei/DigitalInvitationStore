package id.baundang.template.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.*;

import java.util.Map;

public record TemplateRequest(
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Size(max = 200) String slug,
        String description,
        @NotBlank String category,
        String stylePreset,
        @NotNull @Min(1) @Max(3) Short priceLevel,
        String thumbnailUrl,
        JsonNode config,
        Map<String, String> features
) {}
