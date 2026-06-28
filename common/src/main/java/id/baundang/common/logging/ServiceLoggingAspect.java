package id.baundang.common.logging;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Set;

@Aspect
@Component
public class ServiceLoggingAspect {

    private static final Set<String> SENSITIVE = Set.of(
            "password", "passwordhash", "token", "secret", "key", "credential"
    );

    @Around("within(@org.springframework.stereotype.Service *)" +
            " || within(@org.springframework.stereotype.Repository *)")
    public Object logServiceMethod(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        Logger log = LoggerFactory.getLogger(pjp.getTarget().getClass());
        String method = sig.getDeclaringType().getSimpleName() + "#" + sig.getName();

        log.debug("→ {} args={}", method, maskArgs(sig.getParameterNames(), pjp.getArgs()));

        long start = System.currentTimeMillis();
        try {
            Object result = pjp.proceed();
            log.debug("← {} {}ms", method, System.currentTimeMillis() - start);
            return result;
        } catch (Throwable ex) {
            log.warn("✗ {} {}ms threw {}: {}", method,
                    System.currentTimeMillis() - start,
                    ex.getClass().getSimpleName(), ex.getMessage());
            throw ex;
        }
    }

    private String maskArgs(String[] names, Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            String name = (names != null && i < names.length) ? names[i].toLowerCase() : "";
            boolean sensitive = SENSITIVE.stream().anyMatch(name::contains);
            sb.append(sensitive ? "***" : args[i]);
        }
        return sb.append("]").toString();
    }
}
