package id.baundang.invitation.messaging;

import id.baundang.invitation.service.InvitationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class GiftPaidConsumer {

    private final InvitationService invitationService;

    @RabbitListener(queues = "invitation.gift.paid")
    public void onGiftPaid(Map<String, Object> event) {
        try {
            UUID invitationId = UUID.fromString(event.get("invitationId").toString());
            String senderName = event.get("senderName").toString();
            long amount = Long.parseLong(event.get("amount").toString());
            String message = event.containsKey("message") ? event.get("message").toString() : null;
            String midtransOrderId = event.get("midtransOrderId").toString();

            invitationService.recordGiftPaid(invitationId, senderName, amount,
                    message, midtransOrderId);
        } catch (Exception e) {
            log.error("Failed to handle gift.paid event: {}", e.getMessage(), e);
        }
    }
}
