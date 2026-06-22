package id.baundang.admin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TemplateDTO(
        UUID id,
        String name,
        String slug,
        String description,
        String category,
        String stylePreset,
        int priceLevel,
        String thumbnailUrl,
        boolean active
) {}
