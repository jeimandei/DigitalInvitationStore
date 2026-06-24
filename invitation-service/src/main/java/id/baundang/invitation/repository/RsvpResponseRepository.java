package id.baundang.invitation.repository;

import id.baundang.invitation.domain.RsvpResponse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RsvpResponseRepository extends JpaRepository<RsvpResponse, UUID> {

    List<RsvpResponse> findByInvitationIdOrderBySubmittedAtDesc(UUID invitationId);
}
