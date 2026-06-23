CREATE TABLE gift_accounts (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    invitation_id    UUID         NOT NULL UNIQUE REFERENCES invitations (id) ON DELETE CASCADE,
    bank_name        VARCHAR(100),
    account_number   VARCHAR(50),
    account_holder   VARCHAR(200),
    gopay_number     VARCHAR(20),
    ovo_number       VARCHAR(20),
    qris_image_url   VARCHAR(512),
    created_at       TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE gift_confirmations (
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    invitation_id       UUID         NOT NULL REFERENCES invitations (id) ON DELETE CASCADE,
    sender_name         VARCHAR(200) NOT NULL,
    amount              BIGINT       NOT NULL,
    bank_from           VARCHAR(100),
    transfer_proof_url  VARCHAR(512),
    message             TEXT,
    confirmed           BOOLEAN      NOT NULL DEFAULT false,
    created_at          TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_gift_confirmations_invitation_id ON gift_confirmations (invitation_id);
