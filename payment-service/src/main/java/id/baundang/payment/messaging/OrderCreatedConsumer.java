package id.baundang.payment.messaging;

import id.baundang.payment.dto.ChargeRequest;
import id.baundang.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreatedConsumer {

    private final PaymentService paymentService;

    @RabbitListener(queues = "#{orderCreatedQueue.name}")
    public void onOrderCreated(Map<String, Object> event) {
        try {
            UUID orderId = UUID.fromString(event.get("orderId").toString());
            long amount = ((Number) event.get("amount")).longValue();
            String coupleName = event.get("coupleName").toString();
            String email = event.get("contactEmail").toString();
            String phone = event.get("contactWhatsapp").toString();
            String packageName = event.containsKey("packageName")
                    ? event.get("packageName").toString() : null;

            paymentService.charge(new ChargeRequest(orderId, amount, coupleName, email, phone, packageName));
        } catch (Exception e) {
            log.error("Failed to handle order.created event: {}", e.getMessage(), e);
        }
    }
}
