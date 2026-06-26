package id.baundang.template.dto;

import id.baundang.template.domain.Template;

import java.util.UUID;

public record TemplateListDTO(
        UUID id,
        String name,
        String slug,
        String category,
        String stylePreset,
        short priceLevel,
        String thumbnailUrl,
        boolean active
) {
    public static TemplateListDTO from(Template t) {
        return new TemplateListDTO(
                t.getId(),
                t.getName(),
                t.getSlug(),
                t.getCategory().name(),
                t.getStylePreset() != null ? t.getStylePreset().name() : null,
                t.getPriceLevel(),
                t.getThumbnailUrl(),
                t.isActive()
        );
    }
}
