package id.baundang.invitation.repository;

import id.baundang.invitation.domain.Invitation;
import id.baundang.invitation.domain.Invitation.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvitationRepository extends JpaRepository<Invitation, UUID> {

    Optional<Invitation> findByCoupleSlug(String coupleSlug);

    @Modifying
    @Query("UPDATE Invitation i SET i.viewCount = i.viewCount + 1 WHERE i.id = :id")
    void incrementViewCount(UUID id);

    @Query("SELECT i FROM Invitation i WHERE i.status = 'ACTIVE' AND i.activeUntil BETWEEN :from AND :to")
    List<Invitation> findExpiringBetween(LocalDate from, LocalDate to);
}
