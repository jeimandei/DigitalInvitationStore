package id.baundang.invitation.repository;

import id.baundang.invitation.domain.Guest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GuestRepository extends JpaRepository<Guest, UUID> {

    Optional<Guest> findByInviteCode(String inviteCode);

    List<Guest> findAllByInvitationIdOrderByNameAsc(UUID invitationId);

    @Query("SELECT COUNT(g) FROM Guest g WHERE g.invitation.id = :invitationId")
    long countByInvitationId(UUID invitationId);

    @Query("SELECT COUNT(g) FROM Guest g WHERE g.invitation.id = :invitationId AND g.checkedInAt IS NOT NULL")
    long countCheckedInByInvitationId(UUID invitationId);

    @Query("SELECT COALESCE(SUM(g.allottedCount), 0) FROM Guest g WHERE g.invitation.id = :invitationId")
    long sumAllottedByInvitationId(UUID invitationId);

    @Query("SELECT COALESCE(SUM(g.checkedInCount), 0) FROM Guest g "
            + "WHERE g.invitation.id = :invitationId AND g.checkedInAt IS NOT NULL")
    long sumCheckedInCountByInvitationId(UUID invitationId);
}
