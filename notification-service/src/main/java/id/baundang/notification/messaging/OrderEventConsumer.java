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

    @RabbitListener(queues = "notification.order.paid")
    public void onOrderPaid(Map<String, Object> event) {
        try {
            String orderNumber    = event.get("orderNumber").toString();
            String coupleName     = event.get("coupleName") != null ? event.get("coupleName").toString() : "";
            String contactWa      = event.get("contactWhatsapp") != null ? event.get("contactWhatsapp").toString() : "";
            String contactEmail   = event.get("contactEmail") != null ? event.get("contactEmail").toString() : "";
            long   amount         = event.containsKey("amount") ? ((Number) event.get("amount")).longValue() : 0L;
            String dashboardUrl   = "https://baundang.id/pesanan/" + orderNumber;

            // WA to buyer
            if (!contactWa.isBlank()) {
                notificationService.sendWhatsApp(
                        "order.paid.buyer", contactWa,
                        MessageTemplates.orderPaidBuyer(orderNumber, coupleName, dashboardUrl),
                        event
                );
            }

            // WA to admin
            notificationService.sendWhatsApp(
                    "order.paid.admin", adminWhatsapp,
                    MessageTemplates.orderPaidAdmin(orderNumber, coupleName, contactEmail, contactWa, amount),
                    event
            );
        } catch (Exception e) {
            log.error("Failed to handle order.paid event: {}", e.getMessage(), e);
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
