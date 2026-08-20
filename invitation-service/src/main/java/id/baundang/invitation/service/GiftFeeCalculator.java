package id.baundang.invitation.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Nets settlement fees off a gift so the couple's total matches what actually
 * reaches their account.
 *
 * <p>QRIS carries a percentage fee on transactions above a threshold. Reporting
 * gross totals meant the figure in the portal never reconciled against the bank
 * statement, which is the number a couple actually cares about. Rate and
 * threshold are configuration rather than constants because the scheme's pricing
 * changes independently of this codebase.
 */
@Component
public class GiftFeeCalculator {

    /** Midtrans payment_type for a QRIS settlement. */
    private static final String QRIS = "qris";

    private final long qrisThreshold;
    private final double qrisRate;

    public GiftFeeCalculator(
            @Value("${app.gifts.qris-fee-threshold:500000}") long qrisThreshold,
            @Value("${app.gifts.qris-fee-rate:0.007}") double qrisRate) {
        this.qrisThreshold = qrisThreshold;
        this.qrisRate = qrisRate;
    }

    /**
     * The fee deducted from a gift, in rupiah. Zero unless the gift settled over
     * QRIS above the threshold. Rounded half-up to whole rupiah, matching how the
     * scheme itself bills.
     */
    public long feeFor(long amount, String paymentMethod) {
        if (!isQris(paymentMethod) || amount <= qrisThreshold) {
            return 0L;
        }
        return Math.round(amount * qrisRate);
    }

    /** What actually lands in the couple's account for this gift. */
    public long netFor(long amount, String paymentMethod) {
        return amount - feeFor(amount, paymentMethod);
    }

    private boolean isQris(String paymentMethod) {
        return paymentMethod != null && QRIS.equalsIgnoreCase(paymentMethod.trim());
    }
}
