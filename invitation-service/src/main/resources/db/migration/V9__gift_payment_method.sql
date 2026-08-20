-- Gift totals were gross. QRIS settlements carry a percentage fee above a
-- threshold, so a couple reconciling against their bank statement saw a number
-- that never matched. Netting that off requires knowing how each gift was paid.
--
-- Midtrans reports payment_type on the webhook and payment-service already
-- stores it; it simply was not carried on the gift.paid event until now.
-- Null for gifts recorded before this, which are then treated as untaxed.

ALTER TABLE invitation.gifts ADD COLUMN payment_method VARCHAR(50);
