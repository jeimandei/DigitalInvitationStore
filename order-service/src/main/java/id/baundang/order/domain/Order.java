package id.baundang.order.domain;

import com.fasterxml.jackson.databind.JsonNode;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "orders")
@Getter
@Setter
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_number", nullable = false, unique = true, length = 30)
    private String orderNumber;

    @Column(name = "buyer_id", nullable = false)
    private UUID buyerId;

    @Column(name = "template_id")
    private UUID templateId;

    @Column(nullable = false)
    private Long amount = 0L;

    @Column(nullable = false)
    private Short tier;

    @Column(name = "couple_name", nullable = false, length = 200)
    private String coupleName;

    @Column(name = "contact_whatsapp", nullable = false, length = 20)
    private String contactWhatsapp;

    @Column(name = "contact_email", nullable = false)
    private String contactEmail;

    @Type(JsonBinaryType.class)
    @Column(name = "content_draft", columnDefinition = "jsonb")
    private JsonNode contentDraft;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "order_status_enum")
    @org.hibernate.annotations.JdbcType(org.hibernate.dialect.PostgreSQLEnumJdbcType.class)
    private OrderStatusPg status = OrderStatusPg.PENDING;

    @Column(name = "midtrans_transaction_id")
    private String midtransTransactionId;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "revision_count", nullable = false)
    private Short revisionCount = 0;

    @Column(name = "max_revisions", nullable = false)
    private Short maxRevisions = 0;

    @Column(name = "couple_slug", unique = true)
    private String coupleSlug;

    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public enum OrderStatusPg {
        PENDING, PAID, IN_REVISION, COMPLETED, CANCELLED
    }
}
