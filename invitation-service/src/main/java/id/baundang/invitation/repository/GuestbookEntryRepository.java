package id.baundang.invitation.repository;

import id.baundang.invitation.domain.GuestbookEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GuestbookEntryRepository extends JpaRepository<GuestbookEntry, UUID> {
    List<GuestbookEntry> findAllByInvitationIdAndApprovedTrueOrderByCreatedAtDesc(UUID invitationId);

    List<GuestbookEntry> findAllByInvitationIdOrderByCreatedAtDesc(UUID invitationId);
}
