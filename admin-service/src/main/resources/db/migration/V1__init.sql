CREATE TABLE admin_notes (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type  VARCHAR(50)  NOT NULL,
    entity_id    UUID         NOT NULL,
    note         TEXT         NOT NULL,
    created_by   VARCHAR(255) NOT NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_admin_notes_entity ON admin_notes (entity_type, entity_id);
