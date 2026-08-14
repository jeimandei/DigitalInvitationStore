package id.baundang.invitation.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Issues and verifies the pass that proves a guest cleared an invitation's PIN gate.
 *
 * <p>The PIN itself never leaves the server. On a correct submission the guest receives
 * an HttpOnly cookie holding {@code <expiryEpochSeconds>.<hmac>}, where the HMAC covers
 * the slug and the expiry under a server-side secret. That binds the pass to one
 * invitation and makes it unforgeable without the secret.
 */
@Slf4j
@Service
public class PinGateService {

    private static final String COOKIE_PREFIX = "inv_pin_";
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final byte[] secret;
    private final long ttlMinutes;

    public PinGateService(@Value("${app.invitation.pin-secret}") String pinSecret,
                          @Value("${app.invitation.pin-cookie-ttl-minutes:720}") long ttlMinutes) {
        this.secret = pinSecret.getBytes(StandardCharsets.UTF_8);
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
        return COOKIE_PREFIX + HexFormat.of()
                .formatHex(hmac(slug)).substring(0, 16);
    }

    /** Value for a freshly issued pass. */
    public String issue(String slug) {
        long expiry = Instant.now().plusSeconds(ttlMinutes * 60).getEpochSecond();
        return expiry + "." + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(hmac(slug + "." + expiry));
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
            if (expected.equals(cookie.getName()) && isValidPass(cookie.getValue(), slug)) {
                return true;
            }
        }
        return false;
    }

    private boolean isValidPass(String value, String slug) {
        if (value == null) {
            return false;
        }
        int sep = value.indexOf('.');
        if (sep <= 0) {
            return false;
        }
        String expiryPart = value.substring(0, sep);
        String signature = value.substring(sep + 1);
        long expiry;
        try {
            expiry = Long.parseLong(expiryPart);
        } catch (NumberFormatException e) {
            return false;
        }
        if (Instant.now().getEpochSecond() > expiry) {
            return false;
        }
        String want = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(hmac(slug + "." + expiry));
        return MessageDigest.isEqual(
                want.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8));
    }

    private byte[] hmac(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to sign PIN gate pass", e);
        }
    }
}
