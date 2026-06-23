CREATE TYPE order_status_enum AS ENUM ('PENDING', 'PAID', 'IN_REVISION', 'COMPLETED', 'CANCELLED');

CREATE TABLE orders (
    id                      UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    order_number            VARCHAR(30)     NOT NULL UNIQUE,
    buyer_id                UUID            NOT NULL,
    template_id             UUID            NOT NULL,
    tier                    SMALLINT        NOT NULL CHECK (tier IN (1, 2, 3)),
    couple_name             VARCHAR(200)    NOT NULL,
    contact_whatsapp        VARCHAR(20)     NOT NULL,
    contact_email           VARCHAR(255)    NOT NULL,
    content_draft           JSONB           NOT NULL DEFAULT '{}',
    status                  order_status_enum NOT NULL DEFAULT 'PENDING',
    midtrans_transaction_id VARCHAR(255),
    paid_at                 TIMESTAMP,
    revision_count          SMALLINT        NOT NULL DEFAULT 0,
    max_revisions           SMALLINT        NOT NULL DEFAULT 0,
    couple_slug             VARCHAR(255)    UNIQUE,
    notes                   TEXT,
    created_at              TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at              TIMESTAMP       NOT NULL DEFAULT now()
);

CREATE TABLE order_revisions (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id     UUID        NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    requested_by UUID        NOT NULL,
    changes      JSONB       NOT NULL DEFAULT '{}',
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                             CHECK (status IN ('PENDING', 'IN_PROGRESS', 'DONE')),
    created_at   TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE INDEX idx_orders_buyer_id     ON orders (buyer_id);
CREATE INDEX idx_orders_status       ON orders (status);
CREATE INDEX idx_orders_template_id  ON orders (template_id);
CREATE INDEX idx_revisions_order_id  ON order_revisions (order_id);

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$;

CREATE TRIGGER orders_updated_at
    BEFORE UPDATE ON orders
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
