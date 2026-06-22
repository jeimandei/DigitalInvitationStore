package id.baundang.order.dto;

import id.baundang.order.domain.Order.OrderStatusPg;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(
        @NotNull OrderStatusPg status,
        String midtransTransactionId
) {}
