package id.baundang.payment.dto;

import java.util.UUID;

public record GiftChargeResponse(
        UUID giftPaymentId,
        String snapToken,
        String paymentUrl
) {}
