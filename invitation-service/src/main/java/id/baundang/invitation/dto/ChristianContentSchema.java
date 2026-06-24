package id.baundang.invitation.dto;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Documents the {@code christian} block inside invitations.content JSONB.
 *
 * Expected JSONB shape:
 * {
 *   "christian": {
 *     "bibleVerse": {
 *       "reference":   "1 Korintus 13:4-5",
 *       "translation": "TB",
 *       "text":        "Kasih itu sabar; kasih itu murah hati..."
 *     },
 *     "ceremonyType":  "Holy Matrimony",
 *     "churchName":    "GKI Kebayoran Baru",
 *     "churchAddress": "Jl. Wolter Monginsidi No.1, Jakarta Selatan",
 *     "churchTime":    "10.00 WIB"
 *   }
 * }
 */
public record ChristianContentSchema(
        String verseReference,
        String verseTranslation,
        String verseText,
        String ceremonyType,
        String churchName,
        String churchAddress,
        String churchTime
) {
    public static ChristianContentSchema from(JsonNode content) {
        if (content == null || !content.hasNonNull("christian")) {
            return null;
        }
        JsonNode c = content.get("christian");
        JsonNode v = c.path("bibleVerse");
        return new ChristianContentSchema(
                text(v, "reference"),
                text(v, "translation"),
                text(v, "text"),
                text(c, "ceremonyType", "Holy Matrimony"),
                text(c, "churchName"),
                text(c, "churchAddress"),
                text(c, "churchTime")
        );
    }

    public boolean hasVerse() {
        return verseText != null && !verseText.isBlank();
    }

    public boolean hasChurchInfo() {
        return churchName != null && !churchName.isBlank();
    }

    private static String text(JsonNode n, String field) {
        return text(n, field, "");
    }

    private static String text(JsonNode n, String field, String fallback) {
        return n != null && n.hasNonNull(field) ? n.get(field).asText(fallback) : fallback;
    }
}
