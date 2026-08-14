package id.baundang.invitation.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Issues and verifies the pass that proves a guest cleared an invitation's PIN gate.
 *
 * <p>The PIN itself never leaves the server. On a correct submission the guest receives
 * an HttpOnly cookie holding a {@link HmacSigner} pass bound to the slug, so it cannot
 * be forged without the secret or replayed against another invitation.
 */
@Slf4j
@Service
public class PinGateService {

    private static final String COOKIE_PREFIX = "inv_pin_";

    private final HmacSigner signer;
    private final long ttlMinutes;

    public PinGateService(HmacSigner signer,
                          @Value("${app.invitation.pin-cookie-ttl-minutes:720}") long ttlMinutes) {
        this.signer = signer;
        this.ttlMinutes = ttlMinutes;
    }

    /** True when this invitation is PIN-protected. */
    public boolean isProtected(String storedPin) {
        return storedPin != null && !storedPin.isBlank();
    }

    /**
     * Constant-time PIN comparison, so a wrong guess leaks nothing about how much of
     * the PIN was correct.
     */
    public boolean matches(String storedPin, String submitted) {
        if (!isProtected(storedPin) || submitted == null) {
            return false;
        }
        return MessageDigest.isEqual(
                storedPin.trim().getBytes(StandardCharsets.UTF_8),
                submitted.trim().getBytes(StandardCharsets.UTF_8));
    }

    /** Name of the pass cookie for a slug. Hashed so the raw slug is not echoed in headers. */
    public String cookieName(String slug) {
        return COOKIE_PREFIX + signer.tag(slug);
    }

    /** Value for a freshly issued pass. */
    public String issue(String slug) {
        return signer.issue(slug, cookieMaxAgeSeconds());
    }

    /** Seconds the pass cookie should live for. */
    public int cookieMaxAgeSeconds() {
        return (int) (ttlMinutes * 60);
    }

    /** True when the request already carries a valid, unexpired pass for this slug. */
    public boolean hasValidPass(HttpServletRequest request, String slug) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return false;
        }
        String expected = cookieName(slug);
        for (Cookie cookie : cookies) {
            if (expected.equals(cookie.getName()) && signer.isValid(cookie.getValue(), slug)) {
                return true;
            }
        }
        return false;
    }
}
