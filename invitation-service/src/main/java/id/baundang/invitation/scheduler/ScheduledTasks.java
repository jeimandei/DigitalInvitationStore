package id.baundang.invitation.scheduler;

import id.baundang.invitation.domain.Invitation;
import id.baundang.invitation.domain.Invitation.InvitationStatus;
import id.baundang.invitation.repository.InvitationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledTasks {

    private final InvitationRepository invitationRepository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.invitations-exchange}")
    private String invitationsExchange;

    /** 08:00 WIB (01:00 UTC) — publish invitation.expiring for invitations due within 7 days */
    @Scheduled(cron = "0 0 8 * * *", zone = "Asia/Jakarta")
    @Transactional(readOnly = true)
    public void publishExpiringInvitations() {
        LocalDate today = LocalDate.now();
        LocalDate cutoff = today.plusDays(7);
        List<Invitation> expiring = invitationRepository.findExpiringBetween(today, cutoff);
        log.info("Expiry reminder check: {} invitation(s) expiring before {}", expiring.size(), cutoff);

        for (Invitation inv : expiring) {
            try {
                rabbitTemplate.convertAndSend(invitationsExchange, "invitation.expiring", Map.of(
                        "invitationId", inv.getId().toString(),
                        "coupleSlug", inv.getCoupleSlug(),
                        "activeUntil", inv.getActiveUntil().toString(),
                        "daysLeft", (int) today.until(inv.getActiveUntil(),
                                java.time.temporal.ChronoUnit.DAYS)
                ));
            } catch (Exception e) {
                log.error("Failed to publish invitation.expiring for {}: {}", inv.getId(), e.getMessage());
            }
        }
    }

    /** Midnight UTC — bulk-expire all ACTIVE invitations past their active_until date */
    @Scheduled(cron = "0 0 0 * * *", zone = "UTC")
    @Transactional
    public void expireOverdueInvitations() {
        int count = invitationRepository.expireOverdue(
                InvitationStatus.ACTIVE, InvitationStatus.EXPIRED, LocalDate.now());
        if (count > 0) {
            log.info("Expired {} overdue invitation(s)", count);
        }
    }
}
