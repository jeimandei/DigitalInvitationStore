package id.baundang.invitation.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "gift_accounts")
@Getter
@Setter
public class GiftAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invitation_id", nullable = false, unique = true)
    private Invitation invitation;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "account_number", length = 50)
    private String accountNumber;

    @Column(name = "account_holder", length = 200)
    private String accountHolder;

    @Column(name = "gopay_number", length = 20)
    private String gopayNumber;

    @Column(name = "ovo_number", length = 20)
    private String ovoNumber;

    @Column(name = "qris_image_url", length = 512)
    private String qrisImageUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
