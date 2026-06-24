package id.baundang.common.logging;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

public class RequestLoggingInterceptor implements HandlerInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(RequestLoggingInterceptor.class);
    private static final String ATTR_START = "reqStart";

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        request.setAttribute(ATTR_START, System.currentTimeMillis());

        String method = request.getMethod();
        String path = request.getRequestURI();
        String userId = header(request, "X-User-Id");
        String role = header(request, "X-User-Role");
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

        String method = request.getMethod();
        String path = request.getRequestURI();
        int status = response.getStatus();
        String handlerInfo = resolveHandler(handler);

        if (ex != null) {
            LOG.error("<<< {} {} | handler={} | status={} | {}ms | error={}",
                    method, path, handlerInfo, status, durationMs, ex.getMessage());
        } else {
            LOG.info("<<< {} {} | handler={} | status={} | {}ms",
                    method, path, handlerInfo, status, durationMs);
        }
    }

    private String resolveHandler(Object handler) {
        if (handler instanceof HandlerMethod hm) {
            String className = hm.getBeanType().getSimpleName();
            String methodName = hm.getMethod().getName();
            int line = 0;
            try {
                StackTraceElement[] stack = new Throwable().getStackTrace();
                for (StackTraceElement el : stack) {
                    if (el.getClassName().equals(hm.getBeanType().getName())
                            && el.getMethodName().equals(methodName)) {
                        line = el.getLineNumber();
                        break;
                    }
                }
            } catch (Exception ignored) {
            }
            return line > 0 ? className + "#" + methodName + ":" + line : className + "#" + methodName;
        }
        return handler.getClass().getSimpleName();
    }

    private String header(HttpServletRequest request, String name) {
        String val = request.getHeader(name);
        return val != null ? val : "-";
    }
}
