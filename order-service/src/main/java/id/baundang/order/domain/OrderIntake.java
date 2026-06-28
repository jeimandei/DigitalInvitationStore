package id.baundang.order.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "order_intake")
@Getter
@Setter
public class OrderIntake {

    @Id
    @Column(name = "order_id")
    private UUID orderId;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private JsonNode answers = JsonNodeFactory.instance.objectNode();

    @Column(nullable = false)
    private Boolean submitted = false;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
