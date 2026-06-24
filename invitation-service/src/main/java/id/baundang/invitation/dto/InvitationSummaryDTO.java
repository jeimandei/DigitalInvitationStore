package id.baundang.invitation.dto;

import com.fasterxml.jackson.databind.JsonNode;
import id.baundang.invitation.domain.Invitation;

import java.time.LocalDate;
import java.util.UUID;

public record InvitationSummaryDTO(
        UUID id,
        UUID orderId,
        String coupleSlug,
        UUID templateId,
        JsonNode content,
        String status,
        LocalDate activeUntil,
        long viewCount
) {
    public static InvitationSummaryDTO from(Invitation inv) {
        return new InvitationSummaryDTO(
                inv.getId(),
                inv.getOrderId(),
                inv.getCoupleSlug(),
                inv.getTemplateId(),
                inv.getContent(),
                inv.getStatus() != null ? inv.getStatus().name() : null,
                inv.getActiveUntil(),
                inv.getViewCount() != null ? inv.getViewCount() : 0L
        );
    }
}
