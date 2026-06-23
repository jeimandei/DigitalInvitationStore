ALTER TABLE order_revisions
    DROP CONSTRAINT IF EXISTS order_revisions_status_check;

UPDATE order_revisions SET status = 'REQUESTED'  WHERE status = 'PENDING';
UPDATE order_revisions SET status = 'COMPLETED'   WHERE status = 'DONE';

ALTER TABLE order_revisions
    ALTER COLUMN status SET DEFAULT 'REQUESTED',
    ADD CONSTRAINT order_revisions_status_check
        CHECK (status IN ('REQUESTED', 'IN_PROGRESS', 'COMPLETED'));
