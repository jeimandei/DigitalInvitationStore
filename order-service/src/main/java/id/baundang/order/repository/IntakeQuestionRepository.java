package id.baundang.order.repository;

import id.baundang.order.domain.IntakeQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IntakeQuestionRepository extends JpaRepository<IntakeQuestion, UUID> {
    List<IntakeQuestion> findAllByOrderBySortOrderAsc();
    List<IntakeQuestion> findAllByActiveTrueAndMinTierLessThanEqualOrderBySortOrderAsc(short minTier);
}
