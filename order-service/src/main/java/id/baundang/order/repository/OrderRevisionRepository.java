package id.baundang.order.repository;

import id.baundang.order.domain.OrderRevision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderRevisionRepository extends JpaRepository<OrderRevision, UUID> {
    List<OrderRevision> findAllByOrderIdOrderByCreatedAtAsc(UUID orderId);
}
