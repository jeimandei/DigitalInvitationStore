package id.baundang.payment.domain;

import com.fasterxml.jackson.databind.JsonNode;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter
@Setter
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @Column(name = "midtrans_order_id", nullable = false, unique = true)
    private String midtransOrderId;

    @Column(name = "snap_token")
    private String snapToken;

    @Column(name = "payment_url")
    private String paymentUrl;

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Type(JsonBinaryType.class)
    @Column(name = "raw_notification", columnDefinition = "jsonb")
    private JsonNode rawNotification;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
