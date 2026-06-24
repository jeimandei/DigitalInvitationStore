package id.baundang.template.domain;

import com.fasterxml.jackson.databind.JsonNode;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Type;

import java.util.UUID;

@Entity
@Table(name = "christian_template_configs")
public class ChristianTemplateConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", nullable = false, unique = true)
    private Template template;

    @Column(name = "style_preset", length = 20)
    @Enumerated(EnumType.STRING)
    private Template.StylePreset stylePreset;

    @Column(name = "motif_key", length = 100)
    private String motifKey;

    @Type(JsonBinaryType.class)
    @Column(name = "color_palette", columnDefinition = "jsonb", nullable = false)
    private JsonNode colorPalette;

    @Column(name = "hymn_preset", length = 255)
    private String hymnPreset;

    protected ChristianTemplateConfig() {}

    public UUID getId() {
        return id;
    }

    public Template getTemplate() {
        return template;
    }

    public Template.StylePreset getStylePreset() {
        return stylePreset;
    }

    public String getMotifKey() {
        return motifKey;
    }

    public JsonNode getColorPalette() {
        return colorPalette;
    }

    public String getHymnPreset() {
        return hymnPreset;
    }
}
