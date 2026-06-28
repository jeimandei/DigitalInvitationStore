package id.baundang.order.messaging;

import id.baundang.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderPaidConsumer {

    private final OrderService orderService;

    @RabbitListener(queues = "order.payment.processing")
    public void onPaymentPaid(Map<String, Object> event) {
        try {
            Object orderIdRaw = event.get("orderId");
            if (orderIdRaw == null) {
                log.warn("order.paid event missing orderId, skipping");
                return;
            }
            UUID orderId = UUID.fromString(orderIdRaw.toString());
            String midtransOrderId = event.getOrDefault("midtransOrderId", "").toString();
            Instant paidAt = parseInstant(event.get("paidAt"));

            orderService.markPaid(orderId, midtransOrderId, paidAt);
            log.info("Order {} marked as PAID from payment event", orderId);
        } catch (Exception e) {
            log.error("Failed to process payment.paid event: {}", e.getMessage(), e);
        }
    }

    private Instant parseInstant(Object value) {
        if (value == null) {
            return Instant.now();
        }
        String s = value.toString();
        try {
            return Instant.parse(s);
        } catch (Exception e) {
            // fallback: numeric epoch seconds (e.g. 1.782400701E9)
            return Instant.ofEpochSecond((long) Double.parseDouble(s));
        }
    }
}
