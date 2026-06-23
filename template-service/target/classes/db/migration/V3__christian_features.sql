CREATE TABLE christian_template_configs (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id   UUID         NOT NULL UNIQUE REFERENCES templates (id) ON DELETE CASCADE,
    style_preset  VARCHAR(20)  CHECK (style_preset IN ('GRACE','COVENANT','EDEN','GLORIA')),
    motif_key     VARCHAR(100),
    color_palette JSONB        NOT NULL DEFAULT '{}',
    hymn_preset   VARCHAR(255)
);

CREATE INDEX idx_christian_configs_template ON christian_template_configs (template_id);

CREATE TABLE bible_verses (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    reference   VARCHAR(100) NOT NULL,
    translation VARCHAR(10)  NOT NULL CHECK (translation IN ('NIV','KJV','TB','BIS')),
    text        TEXT         NOT NULL,
    category    VARCHAR(20)  NOT NULL CHECK (category IN ('LOVE','COVENANT','BLESSING'))
);

CREATE INDEX idx_bible_verses_translation ON bible_verses (translation);
CREATE INDEX idx_bible_verses_category    ON bible_verses (category);
CREATE UNIQUE INDEX idx_bible_verses_ref_trans ON bible_verses (reference, translation);
