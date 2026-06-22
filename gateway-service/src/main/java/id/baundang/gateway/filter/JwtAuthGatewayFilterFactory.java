package id.baundang.gateway.filter;

import id.baundang.gateway.security.PublicKeyProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Usage in routes:
 *   - JwtAuth               -> requires any valid JWT (Buyer)
 *   - "JwtAuth=ROLE_ADMIN"  -> requires ROLE_ADMIN claim
 */
@Component
public class JwtAuthGatewayFilterFactory
        extends AbstractGatewayFilterFactory<JwtAuthGatewayFilterFactory.Config> {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthGatewayFilterFactory.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final PublicKeyProvider keyProvider;

    public JwtAuthGatewayFilterFactory(PublicKeyProvider keyProvider) {
        super(Config.class);
        this.keyProvider = keyProvider;
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return List.of("requiredRole");
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
                return unauthorized(exchange, "Missing or malformed Authorization header");
            }

            String token = authHeader.substring(BEARER_PREFIX.length());

            Claims claims;
            try {
                claims = Jwts.parser()
                        .verifyWith(keyProvider.get())
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();
            } catch (JwtException e) {
                log.debug("JWT validation failed: {}", e.getMessage());
                return unauthorized(exchange, "Invalid or expired token");
            }

            String userId = claims.getSubject();
            String userRole = claims.get("role", String.class);

            if (config.getRequiredRole() != null && !config.getRequiredRole().equals(userRole)) {
                return forbidden(exchange, "Insufficient role");
            }

            ServerWebExchange mutated = exchange.mutate()
                    .request(r -> r
                            .header("X-User-Id", userId != null ? userId : "")
                            .header("X-User-Role", userRole != null ? userRole : ""))
                    .build();

            return chain.filter(mutated);
        };
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String reason) {
        log.debug("Unauthorized: {}", reason);
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    private Mono<Void> forbidden(ServerWebExchange exchange, String reason) {
        log.debug("Forbidden: {}", reason);
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        return exchange.getResponse().setComplete();
    }

    public static class Config {
        private String requiredRole;

        public String getRequiredRole() { return requiredRole; }
        public void setRequiredRole(String requiredRole) { this.requiredRole = requiredRole; }
    }
}
