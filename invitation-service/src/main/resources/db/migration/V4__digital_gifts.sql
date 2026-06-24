CREATE TABLE gifts (
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    invitation_id       UUID         NOT NULL REFERENCES invitations (id) ON DELETE CASCADE,
    sender_name         VARCHAR(200) NOT NULL,
    amount              BIGINT       NOT NULL,
    message             TEXT,
    midtrans_order_id   VARCHAR(255) NOT NULL UNIQUE,
    created_at          TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_gifts_invitation_id ON gifts (invitation_id);
