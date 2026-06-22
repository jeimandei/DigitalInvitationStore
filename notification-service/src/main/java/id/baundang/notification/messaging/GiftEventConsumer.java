package id.baundang.notification.messaging;

import id.baundang.notification.service.MessageTemplates;
import id.baundang.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GiftEventConsumer {

    private final NotificationService notificationService;

    @RabbitListener(queues = "notification.gift.confirmed")
    public void onGiftConfirmed(Map<String, Object> event) {
        try {
            String coupleName    = event.get("coupleName").toString();
            String coupleWa      = event.get("coupleWhatsapp").toString();
            String senderName    = event.get("senderName").toString();
            long amount          = ((Number) event.get("amount")).longValue();
            String bankFrom      = event.containsKey("bankFrom") ? event.get("bankFrom").toString() : "";

            if (coupleWa.isBlank()) {
                log.warn("gift.confirmed received but coupleWhatsapp is blank, skipping WA");
                return;
            }

            notificationService.sendWhatsApp(
                    "gift.confirmed.couple", coupleWa,
                    MessageTemplates.giftConfirmedCouple(coupleName, senderName, amount, bankFrom),
                    event
            );
        } catch (Exception e) {
            log.error("Failed to handle gift.confirmed event: {}", e.getMessage(), e);
        }
    }
}
