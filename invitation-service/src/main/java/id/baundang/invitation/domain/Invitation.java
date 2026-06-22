package id.baundang.invitation.domain;

import com.fasterxml.jackson.databind.JsonNode;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "invitations")
@Getter
@Setter
public class Invitation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @Column(name = "couple_slug", nullable = false, unique = true)
    private String coupleSlug;

    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    private JsonNode content;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "invitation_status_enum")
    private InvitationStatus status = InvitationStatus.DRAFT;

    @Column(name = "active_until")
    private LocalDate activeUntil;

    @Column(name = "view_count", nullable = false)
    private Long viewCount = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public enum InvitationStatus { DRAFT, ACTIVE, EXPIRED }
}
