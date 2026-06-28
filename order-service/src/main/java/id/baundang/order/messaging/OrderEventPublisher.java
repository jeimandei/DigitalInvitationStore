package id.baundang.order.messaging;

import id.baundang.order.domain.Order;
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
        publish(revisedKey, Map.of(
                "orderId", order.getId(),
                "orderNumber", order.getOrderNumber(),
                "buyerId", order.getBuyerId(),
                "revisionCount", order.getRevisionCount(),
                "occurredAt", Instant.now()
        ));
    }

    public void publishRevisionCompleted(Order order, id.baundang.order.domain.OrderRevision revision) {
        publish(revisionCompletedKey, Map.of(
                "revisionId", revision.getId(),
                "orderId", order.getId(),
                "orderNumber", order.getOrderNumber(),
                "buyerId", order.getBuyerId(),
                "coupleSlug", order.getCoupleSlug() != null ? order.getCoupleSlug() : "",
                "contactWhatsapp", order.getContactWhatsapp(),
                "occurredAt", Instant.now()
        ));
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
