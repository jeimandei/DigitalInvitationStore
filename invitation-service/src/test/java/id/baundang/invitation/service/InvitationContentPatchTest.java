package id.baundang.invitation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import id.baundang.common.exception.ValidationException;
import id.baundang.invitation.domain.Invitation;
import id.baundang.invitation.repository.GiftAccountRepository;
import id.baundang.invitation.repository.GiftConfirmationRepository;
import id.baundang.invitation.repository.GiftRepository;
import id.baundang.invitation.repository.GuestRepository;
import id.baundang.invitation.repository.GuestbookEntryRepository;
import id.baundang.invitation.repository.InvitationRepository;
import id.baundang.invitation.repository.RsvpResponseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Guards the two content-write paths: the admin patch must not be able to move a
 * tenant, and the client patch must not be able to reach owner-controlled settings.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InvitationContentPatchTest {

    private static final UUID INVITATION_ID = UUID.randomUUID();
    private static final UUID OWNER = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final ObjectMapper mapper = new ObjectMapper();

    @Mock
    InvitationRepository invitationRepository;
    @Mock
    RsvpResponseRepository rsvpRepository;
    @Mock
    GuestbookEntryRepository guestbookRepository;
    @Mock
    GiftAccountRepository giftAccountRepository;
    @Mock
    GiftConfirmationRepository giftConfirmationRepository;
    @Mock
    GuestRepository guestRepository;
    @Mock
    GiftRepository giftRepository;
    @Mock
    RabbitTemplate rabbitTemplate;

    @InjectMocks
    InvitationService invitationService;

    private Invitation invitation;

    @BeforeEach
    void setUp() {
        invitation = new Invitation();
        invitation.setId(INVITATION_ID);
        invitation.setBuyerId(OWNER);
        invitation.setCoupleSlug("budi-sari-abc123");

        ObjectNode content = mapper.createObjectNode();
        content.put("coupleName", "Budi & Sari");
        content.put("stylePreset", "GRACE");
        content.put("accessPin", "246813");
        content.put("colorPalette", "blush");
        invitation.setContent(content);

        when(invitationRepository.findById(INVITATION_ID)).thenReturn(Optional.of(invitation));
        when(invitationRepository.save(any(Invitation.class))).thenAnswer(i -> i.getArgument(0));
    }

    private ObjectNode patch(String... keyValues) {
        ObjectNode node = mapper.createObjectNode();
        for (int i = 0; i < keyValues.length; i += 2) {
            node.put(keyValues[i], keyValues[i + 1]);
        }
        return node;
    }

    // ── Admin path ────────────────────────────────────────────────────────────

    @Test
    void adminPatch_appliesOrdinaryContent() {
        invitationService.updateContent(INVITATION_ID, patch("loveStory", "Bertemu di Bandung"));

        assertEquals("Bertemu di Bandung", invitation.getContent().get("loveStory").asText());
    }

    @Test
    void adminPatch_mayChangeThemeAndPin() {
        invitationService.updateContent(INVITATION_ID, patch("stylePreset", "EDEN", "accessPin", "999"));

        assertEquals("EDEN", invitation.getContent().get("stylePreset").asText());
        assertEquals("999", invitation.getContent().get("accessPin").asText());
    }

    @Test
    void adminPatch_cannotReassignTheTenant() {
        String attacker = "22222222-2222-2222-2222-222222222222";

        invitationService.updateContent(INVITATION_ID, patch("buyerId", attacker));

        assertEquals(OWNER, invitation.getBuyerId());
        assertFalse(invitation.getContent().has("buyerId"));
    }

    @Test
    void adminPatch_cannotRepointSlugViaContent() {
        invitationService.updateContent(INVITATION_ID, patch("coupleSlug", "someone-else", "slug", "x"));

        assertEquals("budi-sari-abc123", invitation.getCoupleSlug());
        assertFalse(invitation.getContent().has("coupleSlug"));
        assertFalse(invitation.getContent().has("slug"));
    }

    @Test
    void adminPatch_nonObjectIsRejectedRatherThanReplacingContent() {
        assertThrows(ValidationException.class,
                () -> invitationService.updateContent(INVITATION_ID, mapper.createArrayNode()));
        assertThrows(ValidationException.class,
                () -> invitationService.updateContent(INVITATION_ID, null));

        // The couple's existing content survives the rejected patch.
        assertEquals("Budi & Sari", invitation.getContent().get("coupleName").asText());
    }

    @Test
    void adminPatch_preservesKeysItDoesNotMention() {
        invitationService.updateContent(INVITATION_ID, patch("loveStory", "Baru"));

        assertEquals("GRACE", invitation.getContent().get("stylePreset").asText());
        assertEquals("Budi & Sari", invitation.getContent().get("coupleName").asText());
    }

    // ── Client path writes a draft, never live content ────────────────────────

    @Test
    void clientPatch_writesToDraftAndLeavesGuestsOnThePublishedVersion() {
        invitationService.updateContentAsClient(INVITATION_ID, patch(
                "coupleName", "Budi & Sari Wijaya",
                "receptionVenue", "Gedung Sabuga"));

        JsonNode draft = invitation.getDraftContent();
        assertEquals("Budi & Sari Wijaya", draft.get("coupleName").asText());
        assertEquals("Gedung Sabuga", draft.get("receptionVenue").asText());
        // Live content — what guests see — is untouched until publish.
        assertEquals("Budi & Sari", invitation.getContent().get("coupleName").asText());
    }

    @Test
    void clientPatch_successiveSavesAccumulateInOneDraft() {
        invitationService.updateContentAsClient(INVITATION_ID, patch("coupleName", "Nama Baru"));
        invitationService.updateContentAsClient(INVITATION_ID, patch("loveStory", "Bertemu di Bandung"));

        JsonNode draft = invitation.getDraftContent();
        assertEquals("Nama Baru", draft.get("coupleName").asText());
        assertEquals("Bertemu di Bandung", draft.get("loveStory").asText());
    }

    @Test
    void clientPatch_cannotChangeThemeOrPin() {
        invitationService.updateContentAsClient(INVITATION_ID, patch(
                "stylePreset", "GLORIA", "colorPalette", "neon", "accessPin", "000000"));

        // Neither the draft nor live content picks up owner-controlled settings.
        assertFalse(invitation.getDraftContent().has("stylePreset"));
        assertFalse(invitation.getDraftContent().has("accessPin"));
        assertEquals("GRACE", invitation.getContent().get("stylePreset").asText());
        assertEquals("blush", invitation.getContent().get("colorPalette").asText());
        assertEquals("246813", invitation.getContent().get("accessPin").asText());
    }

    @Test
    void clientPatch_cannotReassignTheTenant() {
        invitationService.updateContentAsClient(INVITATION_ID,
                patch("buyerId", "22222222-2222-2222-2222-222222222222"));

        assertEquals(OWNER, invitation.getBuyerId());
        assertFalse(invitation.getDraftContent().has("buyerId"));
    }

    @Test
    void clientPatch_appliesAllowedKeysEvenWhenMixedWithForbiddenOnes() {
        invitationService.updateContentAsClient(INVITATION_ID, patch(
                "coupleName", "Nama Baru", "stylePreset", "GLORIA"));

        assertEquals("Nama Baru", invitation.getDraftContent().get("coupleName").asText());
        assertFalse(invitation.getDraftContent().has("stylePreset"));
    }

    @Test
    void clientPatch_nonObjectIsRejected() {
        assertThrows(ValidationException.class,
                () -> invitationService.updateContentAsClient(INVITATION_ID, mapper.createArrayNode()));
    }

    @Test
    void clientPatch_acceptsAGalleryArray() {
        ObjectNode withGallery = mapper.createObjectNode();
        withGallery.putArray("gallery")
                .add("/api/v1/media/public/couples/budi-sari/a.jpg")
                .add("/api/v1/media/public/couples/budi-sari/b.jpg");

        invitationService.updateContentAsClient(INVITATION_ID, withGallery);
        invitationService.publishDraft(INVITATION_ID);

        JsonNode gallery = invitation.getContent().get("gallery");
        assertTrue(gallery.isArray());
        assertEquals(2, gallery.size());
        // Order is the couple's arrangement and must survive the round trip.
        assertEquals("/api/v1/media/public/couples/budi-sari/a.jpg", gallery.get(0).asText());
    }

    // ── Publish / discard ─────────────────────────────────────────────────────

    @Test
    void publish_movesDraftIntoLiveContentAndClearsIt() {
        invitationService.updateContentAsClient(INVITATION_ID, patch("coupleName", "Nama Baru"));

        invitationService.publishDraft(INVITATION_ID);

        assertEquals("Nama Baru", invitation.getContent().get("coupleName").asText());
        assertNull(invitation.getDraftContent());
    }

    @Test
    void publish_preservesOwnerControlledKeys() {
        invitationService.updateContentAsClient(INVITATION_ID, patch("coupleName", "Nama Baru"));

        invitationService.publishDraft(INVITATION_ID);

        assertEquals("GRACE", invitation.getContent().get("stylePreset").asText());
        assertEquals("246813", invitation.getContent().get("accessPin").asText());
    }

    @Test
    void publish_cannotSmuggleForbiddenKeysStoredInAnOlderDraft() {
        // A draft written directly (e.g. before a key left the allowlist) is still
        // filtered at publish time, so live content cannot pick it up.
        ObjectNode rogue = mapper.createObjectNode();
        rogue.put("coupleName", "Nama Baru");
        rogue.put("stylePreset", "GLORIA");
        rogue.put("accessPin", "000000");
        invitation.setDraftContent(rogue);

        invitationService.publishDraft(INVITATION_ID);

        assertEquals("Nama Baru", invitation.getContent().get("coupleName").asText());
        assertEquals("GRACE", invitation.getContent().get("stylePreset").asText());
        assertEquals("246813", invitation.getContent().get("accessPin").asText());
    }

    @Test
    void publish_withoutADraftIsRejected() {
        assertThrows(ValidationException.class,
                () -> invitationService.publishDraft(INVITATION_ID));
    }

    @Test
    void discard_dropsPendingEditsAndLeavesLiveContentIntact() {
        invitationService.updateContentAsClient(INVITATION_ID, patch("coupleName", "Nama Baru"));

        invitationService.discardDraft(INVITATION_ID);

        assertNull(invitation.getDraftContent());
        assertEquals("Budi & Sari", invitation.getContent().get("coupleName").asText());
    }

    // ── Editable projection ───────────────────────────────────────────────────

    @Test
    void editableContent_exposesOnlyClientEditableKeys() {
        var editable = invitationService.editableContent(INVITATION_ID);

        assertTrue(editable.content().has("coupleName"));
        // Owner-controlled settings, and the PIN in particular, never reach the portal.
        assertFalse(editable.content().has("stylePreset"));
        assertFalse(editable.content().has("colorPalette"));
        assertFalse(editable.content().has("accessPin"));
        assertFalse(editable.content().has("buyerId"));
        assertFalse(editable.hasDraft());
    }

    @Test
    void editableContent_prefersTheDraftAndFlagsIt() {
        invitationService.updateContentAsClient(INVITATION_ID, patch("coupleName", "Nama Baru"));

        var editable = invitationService.editableContent(INVITATION_ID);

        assertEquals("Nama Baru", editable.content().get("coupleName").asText());
        assertTrue(editable.hasDraft());
    }

    @Test
    void editableContent_handlesEmptyContent() {
        invitation.setContent(null);

        assertTrue(invitationService.editableContent(INVITATION_ID).content().isObject());
    }

    // ── Preview ───────────────────────────────────────────────────────────────

    @Test
    void previewContent_layersDraftOverLiveWithoutPublishing() {
        invitationService.updateContentAsClient(INVITATION_ID, patch("coupleName", "Nama Baru"));

        JsonNode preview = invitationService.previewContent(INVITATION_ID);

        assertEquals("Nama Baru", preview.get("coupleName").asText());
        // Owner-only settings still render, so the preview is a faithful page.
        assertEquals("GRACE", preview.get("stylePreset").asText());
        // And publishing has not happened.
        assertEquals("Budi & Sari", invitation.getContent().get("coupleName").asText());
    }

    @Test
    void previewContent_withoutADraftMatchesLiveContent() {
        JsonNode preview = invitationService.previewContent(INVITATION_ID);

        assertEquals("Budi & Sari", preview.get("coupleName").asText());
    }
}
