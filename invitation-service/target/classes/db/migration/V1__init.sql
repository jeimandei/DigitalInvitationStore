CREATE TYPE invitation_status_enum AS ENUM ('DRAFT', 'ACTIVE', 'EXPIRED');

CREATE TABLE invitations (
    id           UUID                    PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id     UUID                    NOT NULL UNIQUE,
    couple_slug  VARCHAR(255)            NOT NULL UNIQUE,
    template_id  UUID                    NOT NULL,
    content      JSONB                   NOT NULL DEFAULT '{}',
    status       invitation_status_enum  NOT NULL DEFAULT 'DRAFT',
    active_until DATE,
    view_count   BIGINT                  NOT NULL DEFAULT 0,
    created_at   TIMESTAMP               NOT NULL DEFAULT now(),
    updated_at   TIMESTAMP               NOT NULL DEFAULT now()
);

CREATE TABLE rsvp_responses (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    invitation_id UUID        NOT NULL REFERENCES invitations (id) ON DELETE CASCADE,
    guest_name    VARCHAR(200) NOT NULL,
    phone         VARCHAR(20),
    attendance    VARCHAR(20) NOT NULL CHECK (attendance IN ('hadir', 'tidak_hadir')),
    guest_count   SMALLINT    NOT NULL DEFAULT 1,
    message       TEXT,
    submitted_at  TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE TABLE guestbook_entries (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    invitation_id UUID        NOT NULL REFERENCES invitations (id) ON DELETE CASCADE,
    guest_name    VARCHAR(200) NOT NULL,
    message       TEXT        NOT NULL,
    approved      BOOLEAN     NOT NULL DEFAULT false,
    created_at    TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE INDEX idx_invitations_status        ON invitations (status);
CREATE INDEX idx_invitations_active_until  ON invitations (active_until) WHERE status = 'ACTIVE';
CREATE INDEX idx_rsvp_invitation_id        ON rsvp_responses (invitation_id);
CREATE INDEX idx_guestbook_invitation_id   ON guestbook_entries (invitation_id);
CREATE INDEX idx_guestbook_approved        ON guestbook_entries (invitation_id, approved);

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN NEW.updated_at = now(); RETURN NEW; END;
$$;

CREATE TRIGGER invitations_updated_at
    BEFORE UPDATE ON invitations
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
