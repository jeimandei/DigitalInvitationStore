package id.baundang.order.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "intake_question")
@Getter
@Setter
public class IntakeQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String section = "Umum";

    @Column(nullable = false, length = 300)
    private String label;

    @Column(name = "field_key", nullable = false, unique = true, length = 100)
    private String fieldKey;

    @Column(name = "input_type", nullable = false, length = 30)
    private String inputType = "TEXT";

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private JsonNode options = JsonNodeFactory.instance.arrayNode();

    @Column(name = "min_tier", nullable = false)
    private Short minTier = 1;

    @Column(nullable = false)
    private Boolean required = false;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
