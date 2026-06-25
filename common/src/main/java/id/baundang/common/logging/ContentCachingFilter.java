package id.baundang.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

/**
 * Wraps request and response with caching wrappers so RequestLoggingInterceptor
 * can read bodies after the controller has processed them.
 */
public class ContentCachingFilter extends OncePerRequestFilter {

    static final String ATTR_CACHED_REQUEST  = "cachedRequest";
    static final String ATTR_CACHED_RESPONSE = "cachedResponse";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        ContentCachingRequestWrapper  cachedRequest  = new ContentCachingRequestWrapper(request,  8192);
        ContentCachingResponseWrapper cachedResponse = new ContentCachingResponseWrapper(response);

        request.setAttribute(ATTR_CACHED_REQUEST,  cachedRequest);
        request.setAttribute(ATTR_CACHED_RESPONSE, cachedResponse);

        try {
            chain.doFilter(cachedRequest, cachedResponse);
        } finally {
            cachedResponse.copyBodyToResponse();
        }
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return true;
    }
}
