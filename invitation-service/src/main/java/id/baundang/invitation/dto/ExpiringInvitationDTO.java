package id.baundang.invitation.dto;

import com.fasterxml.jackson.databind.JsonNode;
import id.baundang.invitation.domain.Invitation;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public record ExpiringInvitationDTO(
        UUID invitationId,
        String coupleSlug,
        String invitationTitle,
        String coupleName,
        String coupleWhatsapp,
        LocalDate activeUntil,
        int daysLeft
) {
    public static ExpiringInvitationDTO from(Invitation inv) {
        JsonNode content = inv.getContent();
        String title   = textOf(content, "invitationTitle", "Undangan " + inv.getCoupleSlug());
        String couple  = textOf(content, "coupleName", inv.getCoupleSlug());
        String wa      = textOf(content, "contactWhatsapp", "");
        int days       = (int) ChronoUnit.DAYS.between(LocalDate.now(), inv.getActiveUntil());
        return new ExpiringInvitationDTO(inv.getId(), inv.getCoupleSlug(), title, couple, wa, inv.getActiveUntil(), days);
    }

    private static String textOf(JsonNode node, String field, String fallback) {
        return node != null && node.hasNonNull(field) ? node.get(field).asText(fallback) : fallback;
    }
}
