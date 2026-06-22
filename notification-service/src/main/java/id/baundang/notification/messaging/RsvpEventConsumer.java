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
public class RsvpEventConsumer {

    private final NotificationService notificationService;

    @RabbitListener(queues = "notification.rsvp.submitted")
    public void onRsvpSubmitted(Map<String, Object> event) {
        try {
            String guestName       = event.get("guestName").toString();
            String invitationTitle = event.get("invitationTitle").toString();
            String coupleWhatsapp  = event.get("coupleWhatsapp").toString();
            String attendance      = event.get("attendance").toString();
            int guestCount         = event.containsKey("guestCount")
                    ? ((Number) event.get("guestCount")).intValue() : 1;

            notificationService.sendWhatsApp(
                    "rsvp.submitted.couple", coupleWhatsapp,
                    MessageTemplates.rsvpSubmittedCouple(guestName, invitationTitle, attendance, guestCount),
                    event
            );
        } catch (Exception e) {
            log.error("Failed to handle rsvp.submitted event: {}", e.getMessage(), e);
        }
    }
}
