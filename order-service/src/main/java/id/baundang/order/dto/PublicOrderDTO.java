package id.baundang.order.dto;

import id.baundang.order.domain.Order;

import java.time.Instant;
import java.util.UUID;

public record PublicOrderDTO(
        UUID id,
        String orderNumber,
        short tier,
        long amount,
        String coupleName,
        String status,
        short revisionCount,
        short maxRevisions,
        String coupleSlug,
        Instant createdAt
) {
    public static PublicOrderDTO from(Order o) {
        return new PublicOrderDTO(
                o.getId(),
                o.getOrderNumber(),
                o.getTier(),
                o.getAmount() != null ? o.getAmount() : 0L,
                o.getCoupleName(),
                o.getStatus().name(),
                o.getRevisionCount(),
                o.getMaxRevisions(),
                o.getCoupleSlug(),
                o.getCreatedAt()
        );
    }
}
