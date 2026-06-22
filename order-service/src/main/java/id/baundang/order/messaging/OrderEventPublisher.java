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

    public void publishOrderCreated(Order order) {
        publish(createdKey, Map.of(
                "orderId", order.getId(),
                "orderNumber", order.getOrderNumber(),
                "buyerId", order.getBuyerId(),
                "templateId", order.getTemplateId(),
                "tier", order.getTier(),
                "coupleName", order.getCoupleName(),
                "contactEmail", order.getContactEmail(),
                "contactWhatsapp", order.getContactWhatsapp(),
                "occurredAt", Instant.now()
        ));
    }

    public void publishOrderPaid(Order order) {
        publish(paidKey, Map.of(
                "orderId", order.getId(),
                "orderNumber", order.getOrderNumber(),
                "buyerId", order.getBuyerId(),
                "templateId", order.getTemplateId(),
                "tier", order.getTier(),
                "paidAt", order.getPaidAt(),
                "midtransTransactionId", order.getMidtransTransactionId() != null
                        ? order.getMidtransTransactionId() : "",
                "occurredAt", Instant.now()
        ));
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

    private void publish(String routingKey, Object payload) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, payload);
        } catch (Exception e) {
            log.error("Failed to publish event with routing key {}: {}", routingKey, e.getMessage());
        }
    }
}
