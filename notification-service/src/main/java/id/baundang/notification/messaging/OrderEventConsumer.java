package id.baundang.notification.messaging;

import id.baundang.notification.service.MessageTemplates;
import id.baundang.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final NotificationService notificationService;

    @Value("${app.admin.whatsapp}")
    private String adminWhatsapp;

    @RabbitListener(queues = "notification.order.created")
    public void onOrderCreated(Map<String, Object> event) {
        try {
            if (event.get("orderNumber") == null) return;
            String orderNumber  = event.get("orderNumber").toString();
            String orderId      = event.getOrDefault("orderId", "").toString();
            String coupleName   = event.getOrDefault("coupleName", "").toString();
            String contactWa    = event.getOrDefault("contactWhatsapp", "").toString();
            String contactEmail = event.getOrDefault("contactEmail", "").toString();
            long   amount       = event.get("amount") instanceof Number n ? n.longValue() : 0L;
            String paymentUrl   = orderId.isBlank()
                    ? "https://baundang.id/lacak"
                    : "https://baundang.id/bayar/" + orderId;

            if (!contactEmail.isBlank()) {
                notificationService.sendEmail(
                        "order.created.buyer.email", contactEmail,
                        "Pesanan Diterima — " + orderNumber,
                        MessageTemplates.orderCreatedEmailBuyer(orderNumber, coupleName, amount, paymentUrl),
                        event
                );
            }
            if (!contactWa.isBlank()) {
                notificationService.sendWhatsApp(
                        "order.created.buyer", contactWa,
                        MessageTemplates.orderCreatedBuyer(orderNumber, coupleName, paymentUrl),
                        event
                );
            }
        } catch (Exception e) {
            log.error("Failed to handle order.created event: {}", e.getMessage(), e);
        }
    }

    @RabbitListener(queues = "notification.order.paid")
    public void onOrderPaid(Map<String, Object> event) {
        try {
            // Payment-service publishes a bare event (no orderNumber/coupleName).
            // Order-service publishes a rich event after auto-updating the order to PAID.
            // Skip bare events so we only notify once (from the rich order-service event).
            if (event.get("orderNumber") == null) {
                log.debug("Skipping bare payment event (no orderNumber) — waiting for order-service event");
                return;
            }

            String orderNumber  = event.get("orderNumber").toString();
            String coupleName   = event.getOrDefault("coupleName", "").toString();
            String contactWa    = event.getOrDefault("contactWhatsapp", "").toString();
            String contactEmail = event.getOrDefault("contactEmail", "").toString();
            long   amount       = event.containsKey("amount") ? ((Number) event.get("amount")).longValue() : 0L;
            String dashboardUrl = "https://baundang.id/pesanan/" + orderNumber;

            // WhatsApp to buyer
            if (!contactWa.isBlank()) {
                notificationService.sendWhatsApp(
                        "order.paid.buyer", contactWa,
                        MessageTemplates.orderPaidBuyer(orderNumber, coupleName, dashboardUrl),
                        event
                );
            }

            // Email to buyer
            if (!contactEmail.isBlank()) {
                notificationService.sendEmail(
                        "order.paid.buyer.email", contactEmail,
                        "Pembayaran Diterima — " + orderNumber,
                        MessageTemplates.orderPaidEmailBuyer(orderNumber, coupleName, amount, dashboardUrl),
                        event
                );
            }

            // WhatsApp to admin
            notificationService.sendWhatsApp(
                    "order.paid.admin", adminWhatsapp,
                    MessageTemplates.orderPaidAdmin(orderNumber, coupleName, contactEmail, contactWa, amount),
                    event
            );
        } catch (Exception e) {
            log.error("Failed to handle order.paid event: {}", e.getMessage(), e);
        }
    }

    @RabbitListener(queues = "notification.order.completed")
    public void onOrderCompleted(Map<String, Object> event) {
        try {
            if (event.get("orderNumber") == null) return;
            String orderNumber  = event.get("orderNumber").toString();
            String coupleName   = event.getOrDefault("coupleName", "").toString();
            String contactWa    = event.getOrDefault("contactWhatsapp", "").toString();
            String contactEmail = event.getOrDefault("contactEmail", "").toString();
            String coupleSlug   = event.getOrDefault("coupleSlug", "").toString();
            String invitationUrl = coupleSlug.isBlank()
                    ? "https://baundang.id/lacak"
                    : "https://baundang.id/i/" + coupleSlug;

            if (!contactEmail.isBlank()) {
                notificationService.sendEmail(
                        "order.completed.buyer.email", contactEmail,
                        "Undangan Anda Sudah Siap — " + orderNumber,
                        MessageTemplates.orderCompletedEmailBuyer(orderNumber, coupleName, invitationUrl),
                        event
                );
            }
            if (!contactWa.isBlank()) {
                notificationService.sendWhatsApp(
                        "order.completed.buyer", contactWa,
                        MessageTemplates.orderCompletedBuyer(orderNumber, coupleName, invitationUrl),
                        event
                );
            }
        } catch (Exception e) {
            log.error("Failed to handle order.completed event: {}", e.getMessage(), e);
        }
    }

    @RabbitListener(queues = "notification.revision.completed")
    public void onRevisionCompleted(Map<String, Object> event) {
        try {
            String orderNumber   = event.get("orderNumber").toString();
            String contactWa     = event.getOrDefault("contactWhatsapp", "").toString();
            String coupleSlug    = event.getOrDefault("coupleSlug", "").toString();
            String invitationUrl = coupleSlug.isBlank()
                    ? "https://baundang.id"
                    : "https://baundang.id/i/" + coupleSlug;

            if (!contactWa.isBlank()) {
                notificationService.sendWhatsApp(
                        "revision.completed.buyer", contactWa,
                        MessageTemplates.revisionCompleted(orderNumber, invitationUrl),
                        event
                );
            }
        } catch (Exception e) {
            log.error("Failed to handle revision.completed event: {}", e.getMessage(), e);
        }
    }

    @RabbitListener(queues = "notification.order.revised")
    public void onOrderRevised(Map<String, Object> event) {
        try {
            String orderNumber  = event.get("orderNumber").toString();
            String coupleName   = event.containsKey("coupleName") ? event.get("coupleName").toString() : "";
            int revisionCount   = event.containsKey("revisionCount")
                    ? ((Number) event.get("revisionCount")).intValue() : 1;

            notificationService.sendWhatsApp(
                    "order.revised.admin", adminWhatsapp,
                    MessageTemplates.orderRevisedAdmin(orderNumber, coupleName, revisionCount),
                    event
            );
        } catch (Exception e) {
            log.error("Failed to handle order.revised event: {}", e.getMessage(), e);
        }
    }
}
