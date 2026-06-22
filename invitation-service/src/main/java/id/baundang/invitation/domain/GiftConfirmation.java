package id.baundang.invitation.domain;

import jakarta.persistence.*;
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
