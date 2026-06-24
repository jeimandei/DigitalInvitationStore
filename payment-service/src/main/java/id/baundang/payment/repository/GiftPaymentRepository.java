package id.baundang.payment.repository;

import id.baundang.payment.domain.GiftPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GiftPaymentRepository extends JpaRepository<GiftPayment, UUID> {

    Optional<GiftPayment> findByMidtransOrderId(String midtransOrderId);
}
