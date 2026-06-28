package id.baundang.common.logging;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.nio.charset.StandardCharsets;
import java.util.Set;

public class RequestLoggingInterceptor implements HandlerInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(RequestLoggingInterceptor.class);
    private static final String ATTR_START = "reqStart";
    private static final int MAX_BODY_LENGTH = 2000;

    private static final Set<String> SKIP_BODY_CONTENT_TYPES = Set.of(
            "multipart/form-data", "application/octet-stream", "image/", "video/", "audio/"
    );

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        request.setAttribute(ATTR_START, System.currentTimeMillis());

        String method = request.getMethod();
        String path   = request.getRequestURI();
        String userId = header(request, "X-User-Id");
        String role   = header(request, "X-User-Role");
        String handlerInfo = resolveHandler(handler);

        LOG.info(">>> {} {} | handler={} | user={} role={}", method, path, handlerInfo, userId, role);
        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request,
                                @NonNull HttpServletResponse response,
                                @NonNull Object handler,
                                Exception ex) {
        long start = (long) request.getAttribute(ATTR_START);
        long durationMs = System.currentTimeMillis() - start;

        String method      = request.getMethod();
        String path        = request.getRequestURI();
        int    status      = response.getStatus();
        String handlerInfo = resolveHandler(handler);

        String reqBody  = readRequestBody(request);
        String respBody = readResponseBody(request);

        if (ex != null) {
            LOG.error("<<< {} {} | handler={} | status={} | {}ms | error={}\n  req : {}\n  resp: {}",
                    method, path, handlerInfo, status, durationMs, ex.getMessage(), reqBody, respBody);
        } else {
            LOG.info("<<< {} {} | handler={} | status={} | {}ms\n  req : {}\n  resp: {}",
                    method, path, handlerInfo, status, durationMs, reqBody, respBody);
        }
    }

    private String readRequestBody(HttpServletRequest request) {
        Object cached = request.getAttribute(ContentCachingFilter.ATTR_CACHED_REQUEST);
        if (!(cached instanceof ContentCachingRequestWrapper wrapper)) {
            return "-";
        }
        String contentType = request.getContentType();
        if (contentType != null && shouldSkipBody(contentType)) {
            return "(binary)";
        }
        byte[] bytes = wrapper.getContentAsByteArray();
        if (bytes.length == 0) {
            return "-";
        }
        return truncate(new String(bytes, StandardCharsets.UTF_8));
    }

    private String readResponseBody(HttpServletRequest request) {
        Object cached = request.getAttribute(ContentCachingFilter.ATTR_CACHED_RESPONSE);
        if (!(cached instanceof ContentCachingResponseWrapper wrapper)) {
            return "-";
        }
        byte[] bytes = wrapper.getContentAsByteArray();
        if (bytes.length == 0) {
            return "-";
        }
        return truncate(new String(bytes, StandardCharsets.UTF_8));
    }

    private boolean shouldSkipBody(String contentType) {
        String lower = contentType.toLowerCase();
        return SKIP_BODY_CONTENT_TYPES.stream().anyMatch(lower::startsWith);
    }

    private String truncate(String s) {
        if (s == null) {
            return "-";
        }
        String trimmed = s.trim();
        if (trimmed.isEmpty()) {
            return "-";
        }
        return trimmed.length() > MAX_BODY_LENGTH
                ? trimmed.substring(0, MAX_BODY_LENGTH) + "…"
                : trimmed;
    }

    private String resolveHandler(Object handler) {
        if (handler instanceof HandlerMethod hm) {
            return hm.getBeanType().getSimpleName() + "#" + hm.getMethod().getName();
        }
        return handler.getClass().getSimpleName();
    }

    private String header(HttpServletRequest request, String name) {
        String val = request.getHeader(name);
        return val != null ? val : "-";
    }
}
