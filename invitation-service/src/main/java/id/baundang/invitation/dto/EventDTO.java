package id.baundang.invitation.dto;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Represents one event block from invitations.content JSONB.
 *
 * Expected JSONB shape for each item in content.events[]:
 * {
 *   "name":          "Akad Nikah",
 *   "date":          "Sabtu, 14 Juni 2025",
 *   "time":          "08.00 WIB",
 *   "venue_name":    "Masjid Istiqlal",
 *   "venue_address": "Jl. Taman Wijaya Kusuma, Jakarta Pusat",
 *   "venue_lat":     -6.1702,
 *   "venue_lng":     106.8310,
 *   "dress_code":    "Putih & Hijau Sage"
 * }
 */
public record EventDTO(
        String name,
        String date,
        String time,
        String venueName,
        String venueAddress,
        Double venueLat,
        Double venueLng,
        String dressCode
) {
    public static EventDTO from(JsonNode node) {
        return new EventDTO(
                text(node, "name"),
                text(node, "date"),
                text(node, "time"),
                text(node, "venue_name"),
                text(node, "venue_address"),
                decimal(node, "venue_lat"),
                decimal(node, "venue_lng"),
                text(node, "dress_code")
        );
    }

    public boolean hasCoordinates() {
        return venueLat != null && venueLng != null;
    }

    public String mapsEmbedUrl() {
        return "https://maps.google.com/maps?q=%s,%s&z=15&output=embed"
                .formatted(venueLat, venueLng);
    }

    public String mapsDirectUrl() {
        return "https://maps.google.com/?q=%s,%s".formatted(venueLat, venueLng);
    }

    private static String text(JsonNode n, String field) {
        return n != null && n.hasNonNull(field) ? n.get(field).asText("") : "";
    }

    private static Double decimal(JsonNode n, String field) {
        if (n == null || !n.hasNonNull(field)) {
            return null;
        }
        JsonNode v = n.get(field);
        return v.isNumber() ? v.asDouble() : null;
    }
}
