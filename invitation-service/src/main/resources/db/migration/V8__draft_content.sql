-- Client content edits previously wrote straight to the live invitation, so a
-- half-finished change was immediately visible to every guest holding the link.
--
-- Edits now accumulate in draft_content and only reach `content` on an explicit
-- publish. Null means "no unpublished changes", which is the state every existing
-- invitation starts in.

ALTER TABLE invitation.invitations ADD COLUMN draft_content JSONB;
