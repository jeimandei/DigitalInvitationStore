-- Expand category CHECK constraint and remove style_preset restriction
ALTER TABLE templates DROP CONSTRAINT IF EXISTS templates_category_check;
ALTER TABLE templates DROP CONSTRAINT IF EXISTS templates_check;

ALTER TABLE templates
    ADD CONSTRAINT templates_category_check
        CHECK (category IN ('GENERAL','CHRISTIAN','WEDDING','BIRTHDAY','GRADUATION','CORPORATE','OTHER'));
