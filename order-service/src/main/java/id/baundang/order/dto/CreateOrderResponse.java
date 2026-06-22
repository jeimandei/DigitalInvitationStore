package id.baundang.order.dto;

import java.util.UUID;

public record CreateOrderResponse(
        UUID orderId,
        String orderNumber,
        String paymentToken
) {}
