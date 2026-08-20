package id.baundang.invitation.dto;

import id.baundang.invitation.domain.Gift;

import java.time.Instant;
import java.util.UUID;

/**
 * A single gift. {@code amount} is what the sender paid; {@code netAmount} is what
 * reaches the couple after settlement fees, so the two differ only for QRIS gifts
 * above the fee threshold.
 */
public record GiftEntryDTO(
        UUID id,
        String senderName,
        long amount,
        long feeAmount,
        long netAmount,
        String paymentMethod,
        String message,
        Instant createdAt
) {
    public static GiftEntryDTO from(Gift g, long feeAmount) {
        return new GiftEntryDTO(g.getId(), g.getSenderName(),
                g.getAmount(), feeAmount, g.getAmount() - feeAmount,
                g.getPaymentMethod(), g.getMessage(), g.getCreatedAt());
    }
}
