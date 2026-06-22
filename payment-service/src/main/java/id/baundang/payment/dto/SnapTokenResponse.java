package id.baundang.payment.dto;

public record SnapTokenResponse(
        String snapToken,
        String paymentUrl
) {}
