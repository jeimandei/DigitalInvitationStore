CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE templates (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(200) NOT NULL,
    slug          VARCHAR(200) NOT NULL UNIQUE,
    description   TEXT,
    category      VARCHAR(20)  NOT NULL CHECK (category IN ('GENERAL', 'CHRISTIAN')),
    style_preset  VARCHAR(20)  CHECK (
                      style_preset IS NULL
                      OR (category = 'CHRISTIAN' AND style_preset IN ('GRACE','COVENANT','EDEN','GLORIA'))
                  ),
    price_level   SMALLINT     NOT NULL CHECK (price_level IN (1, 2, 3)),
    thumbnail_url VARCHAR(500),
    config        JSONB        NOT NULL DEFAULT '{}',
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE template_features (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id    UUID         NOT NULL REFERENCES templates (id) ON DELETE CASCADE,
    feature_key    VARCHAR(100) NOT NULL,
    feature_value  VARCHAR(500) NOT NULL
);

CREATE INDEX idx_templates_category    ON templates (category)    WHERE active = TRUE;
CREATE INDEX idx_templates_price_level ON templates (price_level) WHERE active = TRUE;
CREATE INDEX idx_templates_active      ON templates (active);
CREATE INDEX idx_template_features_tid ON template_features (template_id);
