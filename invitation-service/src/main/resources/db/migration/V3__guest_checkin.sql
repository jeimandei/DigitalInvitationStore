CREATE TABLE guests (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    invitation_id    UUID         NOT NULL REFERENCES invitations (id) ON DELETE CASCADE,
    name             VARCHAR(200) NOT NULL,
    invite_code      VARCHAR(32)  NOT NULL UNIQUE,
    group_label      VARCHAR(100),
    table_no         VARCHAR(50),
    allotted_count   SMALLINT     NOT NULL DEFAULT 1,
    checked_in_at    TIMESTAMP,
    checked_in_count SMALLINT     NOT NULL DEFAULT 0
);

CREATE INDEX idx_guests_invitation_id ON guests (invitation_id);
CREATE INDEX idx_guests_invite_code   ON guests (invite_code);
