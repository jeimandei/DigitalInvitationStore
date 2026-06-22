package id.baundang.template.dto;

import id.baundang.template.domain.BibleVerse;

import java.util.UUID;

public record BibleVerseDTO(
        UUID id,
        String reference,
        String translation,
        String text,
        String category
) {
    public static BibleVerseDTO from(BibleVerse v) {
        return new BibleVerseDTO(
                v.getId(),
                v.getReference(),
                v.getTranslation().name(),
                v.getText(),
                v.getCategory().name()
        );
    }
}
