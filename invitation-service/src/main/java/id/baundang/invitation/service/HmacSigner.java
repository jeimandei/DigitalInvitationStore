package id.baundang.invitation.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Mints and verifies short-lived, tamper-evident passes of the form
 * {@code <expiryEpochSeconds>.<hmac>}.
 *
 * <p>Used by both the invitation PIN gate and the owner's draft preview. Neither needs
 * server-side state: the expiry travels inside the value and the HMAC over
 * {@code subject + expiry} binds the pass to one invitation, so it cannot be replayed
 * against another or extended by editing the timestamp.
 */
@Component
public class HmacSigner {

    private static final String ALGORITHM = "HmacSHA256";

    private final byte[] secret;

    public HmacSigner(@Value("${app.invitation.pin-secret}") String secret) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * A pass for {@code subject}, valid for {@code ttlSeconds}.
     */
    public String issue(String subject, long ttlSeconds) {
        long expiry = Instant.now().plusSeconds(ttlSeconds).getEpochSecond();
        return expiry + "." + encode(hmac(subject + "." + expiry));
    }

    /**
     * True when {@code value} is an unexpired pass issued for {@code subject}.
     */
    public boolean isValid(String value, String subject) {
        if (value == null) {
            return false;
        }
        int sep = value.indexOf('.');
        if (sep <= 0) {
            return false;
        }
        long expiry;
        try {
            expiry = Long.parseLong(value.substring(0, sep));
        } catch (NumberFormatException e) {
            return false;
        }
        if (Instant.now().getEpochSecond() > expiry) {
            return false;
        }
        String expected = encode(hmac(subject + "." + expiry));
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                value.substring(sep + 1).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * A stable, non-reversible short tag for {@code subject}, for use in cookie names.
     */
    public String tag(String subject) {
        return HexFormat.of().formatHex(hmac(subject)).substring(0, 16);
    }

    private String encode(byte[] raw) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
    }

    private byte[] hmac(String data) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret, ALGORITHM));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to sign value", e);
        }
    }
}
