package id.baundang.order.repository;

import id.baundang.order.domain.Order;
import id.baundang.order.domain.Order.OrderStatusPg;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    Page<Order> findAllByBuyerId(UUID buyerId, Pageable pageable);

    boolean existsByOrderNumber(String orderNumber);

    @Query("""
            SELECT o FROM Order o
            WHERE (:status IS NULL OR o.status = :status)
              AND (:search IS NULL
                   OR LOWER(o.coupleName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(o.contactEmail) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Order> search(@Param("status") OrderStatusPg status,
                       @Param("search") String search,
                       Pageable pageable);
}
