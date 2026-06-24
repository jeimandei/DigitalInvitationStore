package id.baundang.invitation.repository;

import id.baundang.invitation.domain.Gift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface GiftRepository extends JpaRepository<Gift, UUID> {

    List<Gift> findAllByInvitationIdOrderByCreatedAtDesc(UUID invitationId);

    @Query("SELECT COUNT(g) FROM Gift g WHERE g.invitation.id = :invitationId")
    long countByInvitationId(UUID invitationId);

    @Query("SELECT COALESCE(SUM(g.amount), 0) FROM Gift g WHERE g.invitation.id = :invitationId")
    long sumAmountByInvitationId(UUID invitationId);
}
