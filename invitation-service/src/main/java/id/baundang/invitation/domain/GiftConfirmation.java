package id.baundang.invitation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "gift_confirmations")
@Getter
@Setter
public class GiftConfirmation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invitation_id", nullable = false)
    private Invitation invitation;

    @Column(name = "sender_name", nullable = false, length = 200)
    private String senderName;

    @Column(nullable = false)
    private Long amount;

    @Column(name = "bank_from", length = 100)
    private String bankFrom;

    @Column(name = "transfer_proof_url", length = 512)
    private String transferProofUrl;

    private String message;

    @Column(nullable = false)
    private Boolean confirmed = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
