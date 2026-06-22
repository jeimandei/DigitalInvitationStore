package id.baundang.admin.repository;

import id.baundang.admin.entity.AdminNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AdminNoteRepository extends JpaRepository<AdminNote, UUID> {
    List<AdminNote> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, String entityId);
}
