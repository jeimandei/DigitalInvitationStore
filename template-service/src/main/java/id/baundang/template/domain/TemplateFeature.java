package id.baundang.template.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "template_features")
public class TemplateFeature {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", nullable = false)
    private Template template;

    @Column(name = "feature_key", nullable = false, length = 100)
    private String featureKey;

    @Column(name = "feature_value", nullable = false, length = 500)
    private String featureValue;

    protected TemplateFeature() {}

    public TemplateFeature(Template template, String featureKey, String featureValue) {
        this.template     = template;
        this.featureKey   = featureKey;
        this.featureValue = featureValue;
    }

    public UUID getId() {
        return id;
    }

    public Template getTemplate() {
        return template;
    }

    public String getFeatureKey() {
        return featureKey;
    }

    public String getFeatureValue() {
        return featureValue;
    }

    public void setFeatureKey(String v) {
        this.featureKey = v;
    }

    public void setFeatureValue(String v) {
        this.featureValue = v;
    }
}
