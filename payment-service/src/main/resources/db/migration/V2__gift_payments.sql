CREATE TABLE gift_payments (
    id                   UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    invitation_id        UUID         NOT NULL,
    sender_name          VARCHAR(200) NOT NULL,
    message              TEXT,
    midtrans_order_id    VARCHAR(255) NOT NULL UNIQUE,
    snap_token           VARCHAR(255),
    payment_url          VARCHAR(512),
    amount               BIGINT       NOT NULL,
    status               VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
                                      CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'EXPIRED')),
    payment_method       VARCHAR(50),
    paid_at              TIMESTAMP,
    created_at           TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_gift_payments_invitation_id     ON gift_payments (invitation_id);
CREATE INDEX idx_gift_payments_midtrans_order_id ON gift_payments (midtrans_order_id);
