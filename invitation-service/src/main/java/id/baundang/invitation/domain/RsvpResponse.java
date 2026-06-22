package id.baundang.invitation.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "rsvp_responses")
@Getter
@Setter
public class RsvpResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invitation_id", nullable = false)
    private Invitation invitation;

    @Column(name = "guest_name", nullable = false, length = 200)
    private String guestName;

    @Column(length = 20)
    private String phone;

    @Column(nullable = false, length = 20)
    private String attendance;

    @Column(name = "guest_count", nullable = false)
    private Short guestCount = 1;

    private String message;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt = Instant.now();
}
