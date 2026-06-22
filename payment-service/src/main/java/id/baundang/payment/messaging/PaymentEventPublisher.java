package id.baundang.payment.messaging;

import id.baundang.payment.domain.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.orders-exchange}")
    private String exchange;

    @Value("${app.rabbitmq.routing-key.paid}")
    private String paidKey;

    @Value("${app.rabbitmq.routing-key.payment-failed}")
    private String failedKey;

    public void publishOrderPaid(Payment payment) {
        publish(paidKey, Map.of(
                "orderId", payment.getOrderId(),
                "midtransOrderId", payment.getMidtransOrderId(),
                "paymentMethod", payment.getPaymentMethod() != null ? payment.getPaymentMethod() : "",
                "amount", payment.getAmount(),
                "paidAt", payment.getPaidAt(),
                "occurredAt", Instant.now()
        ));
    }

    public void publishPaymentFailed(Payment payment, String reason) {
        publish(failedKey, Map.of(
                "orderId", payment.getOrderId(),
                "midtransOrderId", payment.getMidtransOrderId(),
                "reason", reason,
                "occurredAt", Instant.now()
        ));
    }

    private void publish(String routingKey, Object payload) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, payload);
        } catch (Exception e) {
            log.error("Failed to publish event with routing key {}: {}", routingKey, e.getMessage());
        }
    }
}
