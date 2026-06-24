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
@Table(name = "guests")
@Getter
@Setter
public class Guest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invitation_id", nullable = false)
    private Invitation invitation;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "invite_code", nullable = false, unique = true, length = 32)
    private String inviteCode;

    @Column(name = "group_label", length = 100)
    private String groupLabel;

    @Column(name = "table_no", length = 50)
    private String tableNo;

    @Column(name = "allotted_count", nullable = false)
    private Short allottedCount = 1;

    @Column(name = "checked_in_at")
    private Instant checkedInAt;

    @Column(name = "checked_in_count", nullable = false)
    private Short checkedInCount = 0;

    public boolean isCheckedIn() {
        return checkedInAt != null;
    }
}
