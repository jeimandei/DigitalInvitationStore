package id.baundang.order.dto;

import com.fasterxml.jackson.databind.JsonNode;
import id.baundang.order.domain.OrderRevision;

import java.time.Instant;
import java.util.UUID;

public record OrderRevisionDTO(
        UUID id,
        UUID orderId,
        UUID requestedBy,
        JsonNode changes,
        String status,
        Instant createdAt
) {
    public static OrderRevisionDTO from(OrderRevision r) {
        return new OrderRevisionDTO(
                r.getId(), r.getOrder().getId(), r.getRequestedBy(),
                r.getChanges(), r.getStatus().name(), r.getCreatedAt()
        );
    }
}
