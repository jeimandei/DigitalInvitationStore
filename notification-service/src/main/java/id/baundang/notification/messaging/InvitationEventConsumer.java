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
public class InvitationEventConsumer {

    private final NotificationService notificationService;

    @RabbitListener(queues = "notification.invitation.expiring")
    public void onInvitationExpiring(Map<String, Object> event) {
        try {
            String coupleName       = event.get("coupleName").toString();
            String invitationTitle  = event.get("invitationTitle").toString();
            String coupleWhatsapp   = event.get("coupleWhatsapp").toString();
            int daysLeft            = ((Number) event.get("daysLeft")).intValue();

            notificationService.sendWhatsApp(
                    "invitation.expiring", coupleWhatsapp,
                    MessageTemplates.invitationExpiring(coupleName, invitationTitle, daysLeft),
                    event
            );
        } catch (Exception e) {
            log.error("Failed to handle invitation.expiring event: {}", e.getMessage(), e);
        }
    }
}
