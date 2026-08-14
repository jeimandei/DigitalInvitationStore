package id.baundang.invitation.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PreviewTokenServiceTest {

    private static final String SECRET = "test-secret-value-for-preview";
    private static final String SLUG = "budi-sari-abc123";

    private final HmacSigner signer = new HmacSigner(SECRET);
    private final PreviewTokenService service = new PreviewTokenService(signer);

    @Test
    void issuedTokenIsAccepted() {
        assertTrue(service.isValid(service.issue(SLUG), SLUG));
    }

    @Test
    void tokenIsBoundToItsInvitation() {
        // A preview link for one couple must not expose another couple's draft.
        assertFalse(service.isValid(service.issue(SLUG), "someone-else-xyz789"));
    }

    @Test
    void tamperedTokenIsRejected() {
        assertFalse(service.isValid(service.issue(SLUG) + "x", SLUG));
    }

    @Test
    void tokenForgedUnderADifferentSecretIsRejected() {
        PreviewTokenService attacker = new PreviewTokenService(new HmacSigner("other-secret"));

        assertFalse(service.isValid(attacker.issue(SLUG), SLUG));
    }

    @Test
    void absentOrBlankTokenIsRejected() {
        assertFalse(service.isValid(null, SLUG));
        assertFalse(service.isValid("", SLUG));
        assertFalse(service.isValid("   ", SLUG));
    }

    @Test
    void aPinGatePassCannotBeReusedAsAPreviewToken() {
        // Both are HMAC passes under the same secret, so they must be namespaced apart:
        // clearing a PIN gate must not also unlock someone's unpublished draft.
        String pinPass = signer.issue(SLUG, 3600);

        assertFalse(service.isValid(pinPass, SLUG));
    }
}
