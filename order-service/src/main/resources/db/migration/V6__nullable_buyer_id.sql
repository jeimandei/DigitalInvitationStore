-- Anonymous checkout previously stored UUID.randomUUID() as buyer_id to satisfy
-- NOT NULL, which permanently orphaned the order: the random value matched no
-- account, so the buyer could never open their portal.
--
-- Anonymous orders now carry a null owner until claimed via POST /api/v1/orders/claim.

ALTER TABLE orders ALTER COLUMN buyer_id DROP NOT NULL;
