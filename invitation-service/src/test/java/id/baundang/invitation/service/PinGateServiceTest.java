package id.baundang.invitation.service;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PinGateServiceTest {

    private static final String SECRET = "test-secret-value-for-pin-gate";
    private static final String SLUG = "budi-sari-abc123";

    private final PinGateService service = new PinGateService(new HmacSigner(SECRET), 720);

    @Test
    void isProtected_onlyWhenPinPresent() {
        assertTrue(service.isProtected("1234"));
        assertFalse(service.isProtected(""));
        assertFalse(service.isProtected("   "));
        assertFalse(service.isProtected(null));
    }

    @Test
    void matches_correctPin() {
        assertTrue(service.matches("1234", "1234"));
        assertTrue(service.matches("1234", " 1234 "));
    }

    @Test
    void matches_rejectsWrongOrAbsentPin() {
        assertFalse(service.matches("1234", "9999"));
        assertFalse(service.matches("1234", ""));
        assertFalse(service.matches("1234", null));
        // An unprotected invitation can never be "matched" into a pass.
        assertFalse(service.matches("", "anything"));
    }

    @Test
    void issuedPassIsAccepted() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(service.cookieName(SLUG), service.issue(SLUG)));

        assertTrue(service.hasValidPass(request, SLUG));
    }

    @Test
    void passIsBoundToItsSlug() {
        // A pass minted for one invitation must not open another.
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(service.cookieName(SLUG), service.issue(SLUG)));

        assertFalse(service.hasValidPass(request, "someone-else-xyz789"));
    }

    @Test
    void tamperedSignatureIsRejected() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(service.cookieName(SLUG), service.issue(SLUG) + "x"));

        assertFalse(service.hasValidPass(request, SLUG));
    }

    @Test
    void expiredPassIsRejected() {
        PinGateService expired = new PinGateService(new HmacSigner(SECRET), -1);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(expired.cookieName(SLUG), expired.issue(SLUG)));

        assertFalse(expired.hasValidPass(request, SLUG));
    }

    @Test
    void passForgedUnderADifferentSecretIsRejected() {
        PinGateService attacker = new PinGateService(new HmacSigner("some-other-secret"), 720);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(service.cookieName(SLUG), attacker.issue(SLUG)));

        assertFalse(service.hasValidPass(request, SLUG));
    }

    @Test
    void noCookiesMeansNoPass() {
        assertFalse(service.hasValidPass(new MockHttpServletRequest(), SLUG));
    }

    @Test
    void malformedPassValueIsRejected() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(service.cookieName(SLUG), "not-a-pass"));

        assertFalse(service.hasValidPass(request, SLUG));
    }

    @Test
    void cookieNameIsStableAndSlugSpecific() {
        assertEquals(service.cookieName(SLUG), service.cookieName(SLUG));
        assertNotEquals(service.cookieName(SLUG), service.cookieName("other-slug"));
    }
}
