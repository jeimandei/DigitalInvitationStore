package id.baundang.admin.dto;

public record TemplateCreateRequest(
        String name,
        String slug,
        String description,
        String category,
        String stylePreset,
        short priceLevel,
        String thumbnailUrl
) {}
