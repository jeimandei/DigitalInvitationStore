package id.baundang.common;

import java.text.Normalizer;
import java.util.UUID;

public final class SlugUtils {

    private SlugUtils() {}

    /**
     * Generates a URL-safe slug from a couple name with a short unique suffix.
     * Example: "Budi & Ani" -> "budi-ani-a3f9"
     */
    public static String generateSlug(String coupleName) {
        if (coupleName == null || coupleName.isBlank()) {
            throw new IllegalArgumentException("coupleName must not be blank");
        }

        String normalized = Normalizer
                .normalize(coupleName, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        String base = normalized
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .trim()
                .replaceAll("\\s+", "-");

        if (base.isEmpty()) {
            base = "undangan";
        }

        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 4);

        return base + "-" + suffix;
    }
}
