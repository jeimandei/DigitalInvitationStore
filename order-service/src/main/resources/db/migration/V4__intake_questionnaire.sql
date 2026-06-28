-- Configurable intake questionnaire + per-order answers

CREATE TABLE intake_question (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    section     VARCHAR(100) NOT NULL DEFAULT 'Umum',
    label       VARCHAR(300) NOT NULL,
    field_key   VARCHAR(100) NOT NULL UNIQUE,
    input_type  VARCHAR(30)  NOT NULL DEFAULT 'TEXT',  -- TEXT, TEXTAREA, DATE, TIME, SELECT, COLOR, NUMBER
    options     JSONB        NOT NULL DEFAULT '[]'::jsonb,
    min_tier    SMALLINT     NOT NULL DEFAULT 1,        -- question shown when order.tier >= min_tier
    required    BOOLEAN      NOT NULL DEFAULT false,
    sort_order  INT          NOT NULL DEFAULT 0,
    active      BOOLEAN      NOT NULL DEFAULT true,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_intake_question_order ON intake_question (sort_order);

CREATE TABLE order_intake (
    order_id    UUID PRIMARY KEY REFERENCES orders (id) ON DELETE CASCADE,
    answers     JSONB        NOT NULL DEFAULT '{}'::jsonb,
    submitted   BOOLEAN      NOT NULL DEFAULT false,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ── Default seed questions ─────────────────────────────────────────────────
INSERT INTO intake_question (section, label, field_key, input_type, options, min_tier, required, sort_order) VALUES
  -- Pasangan (all tiers)
  ('Pasangan', 'Nama Lengkap Mempelai Pria',  'groomFullName',  'TEXT',     '[]', 1, true,  10),
  ('Pasangan', 'Nama Lengkap Mempelai Wanita', 'brideFullName', 'TEXT',     '[]', 1, true,  20),
  ('Pasangan', 'Nama Orang Tua Mempelai Pria',  'groomParents', 'TEXTAREA', '[]', 1, false, 30),
  ('Pasangan', 'Nama Orang Tua Mempelai Wanita','brideParents', 'TEXTAREA', '[]', 1, false, 40),
  -- Acara (all tiers)
  ('Acara', 'Tanggal Akad',          'akadDate',       'DATE', '[]', 1, false, 50),
  ('Acara', 'Waktu Akad',            'akadTime',       'TIME', '[]', 1, false, 60),
  ('Acara', 'Lokasi Akad',           'akadVenue',      'TEXT', '[]', 1, false, 70),
  ('Acara', 'Tanggal Resepsi',       'receptionDate',  'DATE', '[]', 1, false, 80),
  ('Acara', 'Waktu Resepsi',         'receptionTime',  'TIME', '[]', 1, false, 90),
  ('Acara', 'Lokasi Resepsi',        'receptionVenue', 'TEXT', '[]', 1, false, 100),
  -- Desain (all tiers)
  ('Desain', 'Pilihan Palet Warna', 'colorPalette', 'SELECT',
     '["Rose Gold","Sage Green","Dusty Blue","Burgundy","Classic White","Terracotta"]', 1, false, 110),
  -- Cerita & galeri (Standar+)
  ('Cerita', 'Kisah Cinta / Perjalanan Kalian', 'loveStory', 'TEXTAREA', '[]', 2, false, 120),
  ('Galeri', 'Tautan Foto / Catatan Galeri',    'galleryNote', 'TEXTAREA', '[]', 2, false, 130),
  ('Fitur',  'Tautan Google Maps Lokasi',        'mapsEmbedUrl', 'TEXT',    '[]', 2, false, 140),
  -- Premium
  ('Premium', 'Pilihan Musik Latar', 'backgroundMusic', 'SELECT',
     '["Romantic Piano","Acoustic Love","Instrumental Worship","Tanpa Musik"]', 3, false, 150),
  ('Premium', 'Daftar Hadiah / Gift Registry', 'giftRegistry', 'TEXTAREA', '[]', 3, false, 160);
