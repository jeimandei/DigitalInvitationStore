package id.baundang.invitation.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GiftFeeCalculatorTest {

    private static final long THRESHOLD = 500_000L;
    private static final double RATE = 0.007;

    private final GiftFeeCalculator calculator = new GiftFeeCalculator(THRESHOLD, RATE);

    @Test
    void qrisAboveThreshold_isCharged() {
        // 1.000.000 × 0.7% = 7.000
        assertEquals(7_000L, calculator.feeFor(1_000_000L, "qris"));
        assertEquals(993_000L, calculator.netFor(1_000_000L, "qris"));
    }

    @Test
    void qrisAtOrBelowThreshold_isFree() {
        assertEquals(0L, calculator.feeFor(THRESHOLD, "qris"));
        assertEquals(0L, calculator.feeFor(200_000L, "qris"));
        assertEquals(THRESHOLD, calculator.netFor(THRESHOLD, "qris"));
    }

    @Test
    void otherMethodsAreNeverCharged() {
        // Only QRIS carries this fee; a bank transfer of the same size must not be netted.
        assertEquals(0L, calculator.feeFor(1_000_000L, "bank_transfer"));
        assertEquals(0L, calculator.feeFor(1_000_000L, "gopay"));
        assertEquals(1_000_000L, calculator.netFor(1_000_000L, "credit_card"));
    }

    @Test
    void unknownMethodIsTreatedAsUntaxed() {
        // Gifts recorded before payment_method was carried have no method at all.
        assertEquals(0L, calculator.feeFor(1_000_000L, null));
        assertEquals(0L, calculator.feeFor(1_000_000L, ""));
        assertEquals(1_000_000L, calculator.netFor(1_000_000L, null));
    }

    @Test
    void methodMatchingIgnoresCaseAndPadding() {
        assertEquals(7_000L, calculator.feeFor(1_000_000L, "QRIS"));
        assertEquals(7_000L, calculator.feeFor(1_000_000L, " qris "));
    }

    @Test
    void feeRoundsToWholeRupiah() {
        // 750.001 × 0.7% = 5250.007 → 5250
        assertEquals(5_250L, calculator.feeFor(750_001L, "qris"));
    }

    @Test
    void rateIsConfigurableRatherThanBakedIn() {
        GiftFeeCalculator repriced = new GiftFeeCalculator(100_000L, 0.02);

        assertEquals(4_000L, repriced.feeFor(200_000L, "qris"));
    }
}
