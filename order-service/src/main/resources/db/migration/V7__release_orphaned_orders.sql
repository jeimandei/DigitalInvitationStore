-- V6 stopped anonymous checkout from minting a random buyer_id, but orders placed
-- before that still carry one. Those UUIDs match no account, so the claim flow's
-- "only an unclaimed order can be taken" guard locks the buyer out permanently.
--
-- Releasing them means nulling any buyer_id that does not correspond to a real
-- user. An order whose owner was deleted is released too, which is what we want:
-- it becomes claimable again by whoever can prove the contact details on it.
--
-- Guarded on auth.users existing so a fresh database, where service migrations may
-- run in any order, does not fail here. On a fresh database `orders` is empty, so
-- skipping costs nothing.

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'auth' AND table_name = 'users'
    ) THEN
        UPDATE orders
        SET buyer_id = NULL
        WHERE buyer_id IS NOT NULL
          AND NOT EXISTS (
              SELECT 1 FROM auth.users u WHERE u.id = orders.buyer_id
          );
    END IF;
END $$;
