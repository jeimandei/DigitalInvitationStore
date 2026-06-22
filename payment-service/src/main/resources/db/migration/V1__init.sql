CREATE TABLE payments (
    id                   UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id             UUID         NOT NULL UNIQUE,
    midtrans_order_id    VARCHAR(255) NOT NULL UNIQUE,
    snap_token           VARCHAR(255),
    payment_url          VARCHAR(512),
    amount               BIGINT       NOT NULL,
    status               VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
                                      CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'EXPIRED')),
    payment_method       VARCHAR(50),
    paid_at              TIMESTAMP,
    raw_notification     JSONB,
    created_at           TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_payments_order_id          ON payments (order_id);
CREATE INDEX idx_payments_midtrans_order_id ON payments (midtrans_order_id);
CREATE INDEX idx_payments_status            ON payments (status);
