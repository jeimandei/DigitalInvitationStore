-- Promote tenant ownership out of the mutable JSONB content blob into a
-- first-class column. Previously requireOwned() read content->>'buyerId',
-- which the admin content merge-patch could overwrite.
--
-- Stays nullable: legacy rows may carry no valid buyerId, and orders placed
-- anonymously have no owner until they are claimed.

ALTER TABLE invitation.invitations ADD COLUMN buyer_id UUID;

UPDATE invitation.invitations
SET buyer_id = (content ->> 'buyerId')::uuid
WHERE content ->> 'buyerId' ~
      '^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$';

CREATE INDEX idx_invitations_buyer_id ON invitation.invitations (buyer_id);
