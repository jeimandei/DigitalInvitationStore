package id.baundang.invitation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Short-lived tokens that let an owner view their unpublished draft on the real
 * invitation page.
 *
 * <p>The invitation page is a plain browser navigation, so it cannot carry the buyer's
 * JWT (which lives in sessionStorage, not a cookie). Instead the portal — which is
 * authenticated and ownership-checked — mints a token bound to the slug, and the page
 * accepts it via {@code ?preview=}. The short TTL keeps a leaked link from becoming a
 * lasting way to read someone's unpublished changes.
 */
@Service
@RequiredArgsConstructor
public class PreviewTokenService {

    /** Long enough to look over a draft, short enough that a shared link goes stale. */
    private static final long TTL_SECONDS = 30 * 60;

    private static final String SUBJECT_PREFIX = "preview:";

    private final HmacSigner signer;

    public String issue(String slug) {
        return signer.issue(SUBJECT_PREFIX + slug, TTL_SECONDS);
    }

    public boolean isValid(String token, String slug) {
        return token != null && !token.isBlank()
                && signer.isValid(token, SUBJECT_PREFIX + slug);
    }

    public long ttlSeconds() {
        return TTL_SECONDS;
    }
}
