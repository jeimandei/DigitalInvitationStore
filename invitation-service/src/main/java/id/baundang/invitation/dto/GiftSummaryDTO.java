package id.baundang.invitation.dto;

import java.util.List;

/**
 * Gift totals for the couple. {@code totalAmount} is gross, {@code totalNetAmount}
 * is what reconciles against their bank statement once settlement fees are netted
 * off; they are equal when no gift attracted a fee.
 */
public record GiftSummaryDTO(
        long totalGifts,
        long totalAmount,
        long totalFeeAmount,
        long totalNetAmount,
        List<GiftEntryDTO> entries
) {}
