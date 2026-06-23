CREATE TABLE notifications (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    type         VARCHAR(50) NOT NULL,
    recipient    VARCHAR(255) NOT NULL,
    channel      VARCHAR(20) NOT NULL CHECK (channel IN ('WHATSAPP', 'EMAIL')),
    template_key VARCHAR(100) NOT NULL,
    payload      JSONB       NOT NULL DEFAULT '{}',
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                             CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    sent_at      TIMESTAMP,
    created_at   TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE INDEX idx_notifications_status     ON notifications (status);
CREATE INDEX idx_notifications_type       ON notifications (type);
CREATE INDEX idx_notifications_created_at ON notifications (created_at);
