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

    // ── Client path ───────────────────────────────────────────────────────────

    @Test
    void clientPatch_appliesAllowlistedFields() {
        invitationService.updateContentAsClient(INVITATION_ID, patch(
                "coupleName", "Budi & Sari Wijaya",
                "receptionVenue", "Gedung Sabuga",
                "loveStory", "Bertemu di Bandung"));

        JsonNode content = invitation.getContent();
        assertEquals("Budi & Sari Wijaya", content.get("coupleName").asText());
        assertEquals("Gedung Sabuga", content.get("receptionVenue").asText());
        assertEquals("Bertemu di Bandung", content.get("loveStory").asText());
    }

    @Test
    void clientPatch_cannotChangeTheme() {
        invitationService.updateContentAsClient(INVITATION_ID, patch(
                "stylePreset", "GLORIA", "colorPalette", "neon"));

        assertEquals("GRACE", invitation.getContent().get("stylePreset").asText());
        assertEquals("blush", invitation.getContent().get("colorPalette").asText());
    }

    @Test
    void clientPatch_cannotChangeAccessPin() {
        invitationService.updateContentAsClient(INVITATION_ID, patch("accessPin", "000000"));

        assertEquals("246813", invitation.getContent().get("accessPin").asText());
    }

    @Test
    void clientPatch_cannotReassignTheTenant() {
        invitationService.updateContentAsClient(INVITATION_ID,
                patch("buyerId", "22222222-2222-2222-2222-222222222222"));

        assertEquals(OWNER, invitation.getBuyerId());
        assertFalse(invitation.getContent().has("buyerId"));
    }

    @Test
    void clientPatch_appliesAllowedKeysEvenWhenMixedWithForbiddenOnes() {
        invitationService.updateContentAsClient(INVITATION_ID, patch(
                "coupleName", "Nama Baru", "stylePreset", "GLORIA"));

        assertEquals("Nama Baru", invitation.getContent().get("coupleName").asText());
        assertEquals("GRACE", invitation.getContent().get("stylePreset").asText());
    }

    @Test
    void clientPatch_nonObjectIsRejected() {
        assertThrows(ValidationException.class,
                () -> invitationService.updateContentAsClient(INVITATION_ID, mapper.createArrayNode()));
    }

    // ── Editable projection ───────────────────────────────────────────────────

    @Test
    void editableContent_exposesOnlyClientEditableKeys() {
        JsonNode editable = invitationService.editableContent(INVITATION_ID);

        assertTrue(editable.has("coupleName"));
        // Owner-controlled settings, and the PIN in particular, never reach the portal.
        assertFalse(editable.has("stylePreset"));
        assertFalse(editable.has("colorPalette"));
        assertFalse(editable.has("accessPin"));
        assertFalse(editable.has("buyerId"));
    }

    @Test
    void editableContent_handlesEmptyContent() {
        invitation.setContent(null);

        assertTrue(invitationService.editableContent(INVITATION_ID).isObject());
    }
}
