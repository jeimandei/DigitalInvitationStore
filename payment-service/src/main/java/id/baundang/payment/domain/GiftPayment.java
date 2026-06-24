package id.baundang.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "gift_payments")
@Getter
@Setter
public class GiftPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "invitation_id", nullable = false)
    private UUID invitationId;

    @Column(name = "sender_name", nullable = false, length = 200)
    private String senderName;

    private String message;

    @Column(name = "midtrans_order_id", nullable = false, unique = true, length = 255)
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

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
