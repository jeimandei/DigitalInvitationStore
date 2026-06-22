package id.baundang.invitation.repository;

import id.baundang.invitation.domain.GiftAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GiftAccountRepository extends JpaRepository<GiftAccount, UUID> {
    Optional<GiftAccount> findByInvitationId(UUID invitationId);
}
