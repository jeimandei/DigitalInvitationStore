package id.baundang.template.dto;

import com.fasterxml.jackson.databind.JsonNode;
import id.baundang.template.domain.Template;
import id.baundang.template.domain.TemplateFeature;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public record TemplateDTO(
        UUID id,
        String name,
        String slug,
        String description,
        String category,
        String stylePreset,
        short priceLevel,
        String thumbnailUrl,
        JsonNode config,
        Map<String, String> features,
        boolean active,
        Instant createdAt
) {
    public static TemplateDTO from(Template t) {
        Map<String, String> featureMap = t.getFeatures().stream()
                .collect(Collectors.toMap(
                        TemplateFeature::getFeatureKey,
                        TemplateFeature::getFeatureValue,
                        (a, b) -> b));

        return new TemplateDTO(
                t.getId(),
                t.getName(),
                t.getSlug(),
                t.getDescription(),
                t.getCategory().name(),
                t.getStylePreset() != null ? t.getStylePreset().name() : null,
                t.getPriceLevel(),
                t.getThumbnailUrl(),
                t.getConfig(),
                featureMap,
                t.isActive(),
                t.getCreatedAt()
        );
    }
}
