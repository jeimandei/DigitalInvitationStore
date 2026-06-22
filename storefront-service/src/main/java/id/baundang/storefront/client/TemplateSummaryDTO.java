package id.baundang.storefront.client;

public record TemplateSummaryDTO(
        String id,
        String name,
        String slug,
        String thumbnailUrl,
        String category,
        String priceLevel
) {}
