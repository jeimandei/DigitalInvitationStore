package id.baundang.invitation.repository;

import id.baundang.invitation.domain.GiftConfirmation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GiftConfirmationRepository extends JpaRepository<GiftConfirmation, UUID> {}
