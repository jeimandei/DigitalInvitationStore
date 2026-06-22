package id.baundang.order.dto;

import id.baundang.order.domain.Order;

import java.time.Instant;
import java.util.UUID;

public record OrderDTO(
        UUID id,
        String orderNumber,
        UUID buyerId,
        UUID templateId,
        short tier,
        String coupleName,
        String contactWhatsapp,
        String contactEmail,
        String status,
        String midtransTransactionId,
        Instant paidAt,
        short revisionCount,
        short maxRevisions,
        String coupleSlug,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
    public static OrderDTO from(Order o) {
        return new OrderDTO(
                o.getId(), o.getOrderNumber(), o.getBuyerId(), o.getTemplateId(),
                o.getTier(), o.getCoupleName(), o.getContactWhatsapp(), o.getContactEmail(),
                o.getStatus().name(), o.getMidtransTransactionId(), o.getPaidAt(),
                o.getRevisionCount(), o.getMaxRevisions(), o.getCoupleSlug(), o.getNotes(),
                o.getCreatedAt(), o.getUpdatedAt()
        );
    }
}
