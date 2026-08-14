package id.baundang.order.messaging;

import id.baundang.order.domain.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.routing-key.created}")
    private String createdKey;

    @Value("${app.rabbitmq.routing-key.paid}")
    private String paidKey;

    @Value("${app.rabbitmq.routing-key.revised}")
    private String revisedKey;

    @Value("${app.rabbitmq.routing-key.revision-completed}")
    private String revisionCompletedKey;

    @Value("${app.rabbitmq.routing-key.completed}")
    private String completedKey;

    @Value("${app.rabbitmq.routing-key.claimed}")
    private String claimedKey;

    /** An anonymous order was bound to an account; invitation-service mirrors the owner. */
    public void publishOrderClaimed(Order order) {
        var payload = new java.util.HashMap<String, Object>();
        payload.put("orderId", order.getId());
        payload.put("orderNumber", order.getOrderNumber());
        payload.put("buyerId", order.getBuyerId());
        payload.put("occurredAt", Instant.now());
        publish(claimedKey, payload);
    }

    public void publishOrderCreated(Order order, String packageName) {
        var payload = new java.util.HashMap<String, Object>();
        payload.put("orderId", order.getId());
        payload.put("orderNumber", order.getOrderNumber());
        payload.put("buyerId", order.getBuyerId());
        payload.put("tier", order.getTier());
        payload.put("packageName", packageName);
        payload.put("amount", order.getAmount());
        payload.put("coupleName", order.getCoupleName());
        payload.put("contactEmail", order.getContactEmail());
        payload.put("contactWhatsapp", order.getContactWhatsapp());
        payload.put("occurredAt", Instant.now());
        if (order.getTemplateId() != null) {
            payload.put("templateId", order.getTemplateId());
        }
        publish(createdKey, payload);
    }

    public void publishOrderPaid(Order order) {
        var payload = new java.util.HashMap<String, Object>();
        payload.put("orderId", order.getId());
        payload.put("orderNumber", order.getOrderNumber());
        payload.put("buyerId", order.getBuyerId());
        payload.put("tier", order.getTier());
        payload.put("amount", order.getAmount() != null ? order.getAmount() : 0L);
        payload.put("coupleName", order.getCoupleName());
        payload.put("contactWhatsapp", order.getContactWhatsapp() != null ? order.getContactWhatsapp() : "");
        payload.put("contactEmail", order.getContactEmail() != null ? order.getContactEmail() : "");
        payload.put("coupleSlug", order.getCoupleSlug() != null ? order.getCoupleSlug() : "");
        payload.put("paidAt", order.getPaidAt());
        payload.put("midtransTransactionId", order.getMidtransTransactionId() != null
                ? order.getMidtransTransactionId() : "");
        if (order.getTemplateId() != null) {
            payload.put("templateId", order.getTemplateId());
        }
        payload.put("occurredAt", Instant.now());
        publish(paidKey, payload);
    }

    public void publishOrderRevised(Order order) {
        // HashMap rather than Map.of: buyerId is null on an unclaimed order and
        // Map.of throws NPE on null values.
        var payload = new java.util.HashMap<String, Object>();
        payload.put("orderId", order.getId());
        payload.put("orderNumber", order.getOrderNumber());
        payload.put("buyerId", order.getBuyerId());
        payload.put("revisionCount", order.getRevisionCount());
        payload.put("occurredAt", Instant.now());
        publish(revisedKey, payload);
    }

    public void publishRevisionCompleted(Order order, id.baundang.order.domain.OrderRevision revision) {
        // HashMap rather than Map.of: buyerId is null on an unclaimed order and
        // Map.of throws NPE on null values.
        var payload = new java.util.HashMap<String, Object>();
        payload.put("revisionId", revision.getId());
        payload.put("orderId", order.getId());
        payload.put("orderNumber", order.getOrderNumber());
        payload.put("buyerId", order.getBuyerId());
        payload.put("coupleSlug", order.getCoupleSlug() != null ? order.getCoupleSlug() : "");
        payload.put("contactWhatsapp", order.getContactWhatsapp());
        payload.put("occurredAt", Instant.now());
        publish(revisionCompletedKey, payload);
    }

    public void publishOrderCompleted(Order order) {
        var payload = new java.util.HashMap<String, Object>();
        payload.put("orderId", order.getId());
        payload.put("orderNumber", order.getOrderNumber());
        payload.put("buyerId", order.getBuyerId());
        payload.put("coupleName", order.getCoupleName() != null ? order.getCoupleName() : "");
        payload.put("contactEmail", order.getContactEmail() != null ? order.getContactEmail() : "");
        payload.put("contactWhatsapp", order.getContactWhatsapp() != null ? order.getContactWhatsapp() : "");
        payload.put("coupleSlug", order.getCoupleSlug() != null ? order.getCoupleSlug() : "");
        payload.put("occurredAt", Instant.now());
        publish(completedKey, payload);
    }

    private void publish(String routingKey, Object payload) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, payload);
        } catch (Exception e) {
            log.error("Failed to publish event with routing key {}: {}", routingKey, e.getMessage());
        }
    }
}
