package id.baundang.template.domain;

import com.fasterxml.jackson.databind.JsonNode;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "templates")
public class Template {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, unique = true, length = 200)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Category category;

    @Column(name = "style_preset", length = 20)
    @Enumerated(EnumType.STRING)
    private StylePreset stylePreset;

    @Column(name = "price_level", nullable = false)
    private short priceLevel;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    private JsonNode config;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<TemplateFeature> features = new ArrayList<>();

    public enum Category { GENERAL, CHRISTIAN, WEDDING, BIRTHDAY, GRADUATION, CORPORATE, OTHER }

    public enum StylePreset { GRACE, COVENANT, EDEN, GLORIA }

    public Template() {}

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    public String getDescription() {
        return description;
    }

    public Category getCategory() {
        return category;
    }

    public StylePreset getStylePreset() {
        return stylePreset;
    }

    public short getPriceLevel() {
        return priceLevel;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public JsonNode getConfig() {
        return config;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<TemplateFeature> getFeatures() {
        return features;
    }

    public void setName(String v) {
        this.name = v;
    }

    public void setSlug(String v) {
        this.slug = v;
    }

    public void setDescription(String v) {
        this.description = v;
    }

    public void setCategory(Category v) {
        this.category = v;
    }

    public void setStylePreset(StylePreset v) {
        this.stylePreset = v;
    }

    public void setPriceLevel(short v) {
        this.priceLevel = v;
    }

    public void setThumbnailUrl(String v) {
        this.thumbnailUrl = v;
    }

    public void setConfig(JsonNode v) {
        this.config = v;
    }

    public void setActive(boolean v) {
        this.active = v;
    }
}
