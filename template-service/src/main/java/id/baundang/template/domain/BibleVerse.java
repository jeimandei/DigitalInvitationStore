package id.baundang.template.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "bible_verses")
public class BibleVerse {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String reference;

    @Column(nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private Translation translation;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Category category;

    public enum Translation { NIV, KJV, TB, BIS }

    public enum Category { LOVE, COVENANT, BLESSING }

    protected BibleVerse() {}

    public UUID getId() {
        return id;
    }

    public String getReference() {
        return reference;
    }

    public Translation getTranslation() {
        return translation;
    }

    public String getText() {
        return text;
    }

    public Category getCategory() {
        return category;
    }
}
