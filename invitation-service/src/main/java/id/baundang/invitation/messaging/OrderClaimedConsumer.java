package id.baundang.invitation.messaging;

import id.baundang.invitation.domain.Invitation;
import id.baundang.invitation.repository.InvitationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Mirrors an order claim onto its invitation.
 *
 * <p>The invitation is created when the order is paid, which can be long before an
 * anonymous buyer binds that order to an account. Without this consumer the invitation
 * would keep a null owner and the client portal would stay locked after a successful
 * claim.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderClaimedConsumer {

    private final InvitationRepository invitationRepository;

    @RabbitListener(queues = "invitation.order.claimed")
    @Transactional
    public void onOrderClaimed(Map<String, Object> event) {
        try {
            Object orderIdRaw = event.get("orderId");
            Object buyerIdRaw = event.get("buyerId");
            if (orderIdRaw == null || buyerIdRaw == null) {
                log.warn("Ignoring order.claimed with missing orderId/buyerId: {}", event);
                return;
            }
            UUID orderId = UUID.fromString(orderIdRaw.toString());
            UUID buyerId = UUID.fromString(buyerIdRaw.toString());

            Invitation inv = invitationRepository.findByOrderId(orderId).orElse(null);
            if (inv == null) {
                // The order was claimed before payment created the invitation; the
                // buyerId on the later order.paid event carries the owner instead.
                log.info("No invitation yet for claimed order {}", orderId);
                return;
            }
            inv.setBuyerId(buyerId);
            invitationRepository.save(inv);
            log.info("Invitation {} now owned by buyer {}", inv.getId(), buyerId);
        } catch (Exception e) {
            log.error("Failed to handle order.claimed event: {}", e.getMessage(), e);
        }
    }
}
