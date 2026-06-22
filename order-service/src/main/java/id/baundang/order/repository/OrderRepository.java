package id.baundang.order.repository;

import id.baundang.order.domain.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    Page<Order> findAllByBuyerId(UUID buyerId, Pageable pageable);
    boolean existsByOrderNumber(String orderNumber);
}
