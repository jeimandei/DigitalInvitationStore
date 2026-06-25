package id.baundang.invitation.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.baundang.invitation.domain.Invitation;
import id.baundang.invitation.domain.Invitation.InvitationStatus;
import id.baundang.invitation.repository.InvitationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderPaidConsumer {

    private static final int ACTIVE_DAYS = 180;

    private final InvitationRepository invitationRepository;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "invitation.order.paid")
    public void onOrderPaid(Map<String, Object> event) {
        try {
            UUID orderId = UUID.fromString(event.get("orderId").toString());
            String slug  = buildSlug(event);

            if (invitationRepository.findByCoupleSlug(slug).isPresent()) {
                log.warn("Invitation already exists for slug {}", slug);
                return;
            }

            Object templateIdRaw = event.get("templateId");
            UUID templateId = (templateIdRaw != null && !templateIdRaw.toString().isBlank())
                    ? UUID.fromString(templateIdRaw.toString()) : null;

            Invitation invitation = new Invitation();
            invitation.setOrderId(orderId);
            invitation.setCoupleSlug(slug);
            invitation.setTemplateId(templateId);
            invitation.setContent(objectMapper.valueToTree(event));
            invitation.setStatus(InvitationStatus.ACTIVE);
            invitation.setActiveUntil(LocalDate.now().plusDays(ACTIVE_DAYS));

            invitationRepository.save(invitation);
            log.info("Created invitation for order {} with slug {}", orderId, slug);
        } catch (Exception e) {
            log.error("Failed to handle order.paid event: {}", e.getMessage(), e);
        }
    }

    private String buildSlug(Map<String, Object> event) {
        String raw = event.containsKey("coupleName")
                ? event.get("coupleName").toString()
                : event.get("orderId").toString();
        String slug = raw.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-");
        String suffix = event.get("orderId").toString().replace("-", "").substring(0, 6);
        return slug + "-" + suffix;
    }
}
