package id.baundang.payment.dto;

import java.util.UUID;

public record ChargeResponse(
        UUID paymentId,
        UUID orderId,
        String snapToken,
        String paymentUrl
) {}
