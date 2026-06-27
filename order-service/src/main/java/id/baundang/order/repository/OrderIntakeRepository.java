package id.baundang.order.repository;

import id.baundang.order.domain.OrderIntake;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderIntakeRepository extends JpaRepository<OrderIntake, UUID> {
}
