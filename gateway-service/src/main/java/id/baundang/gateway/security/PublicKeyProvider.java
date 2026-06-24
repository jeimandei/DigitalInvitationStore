package id.baundang.gateway.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import jakarta.annotation.PostConstruct;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.util.Base64;

@Component
public class PublicKeyProvider {

    private static final Logger LOG = LoggerFactory.getLogger(PublicKeyProvider.class);

    @Value("${app.auth.public-key-uri}")
    private String publicKeyUri;

    private volatile PublicKey publicKey;

    private final WebClient webClient;

    public PublicKeyProvider(WebClient.Builder builder) {
        this.webClient = builder.build();
    }

    @PostConstruct
    public void fetchPublicKey() {
        webClient.get()
                .uri(publicKeyUri)
                .retrieve()
                .bodyToMono(String.class)
                .retryWhen(Retry.backoff(6, Duration.ofSeconds(5))
                        .maxBackoff(Duration.ofSeconds(30))
                        .doBeforeRetry(sig -> LOG.warn("Retrying public key fetch, attempt {}",
                                sig.totalRetries() + 1)))
                .flatMap(this::parse)
                .doOnSuccess(key -> {
                    this.publicKey = key;
                    LOG.info("Auth service public key loaded successfully");
                })
                .doOnError(e -> LOG.error("Failed to load auth service public key", e))
                .subscribe();
    }

    private Mono<PublicKey> parse(String pem) {
        try {
            String stripped = pem
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] decoded = Base64.getDecoder().decode(stripped);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return Mono.just(kf.generatePublic(new X509EncodedKeySpec(decoded)));
        } catch (Exception e) {
            return Mono.error(new IllegalStateException("Cannot parse auth service public key", e));
        }
    }

    public PublicKey get() {
        if (publicKey == null) {
            throw new IllegalStateException("Public key not yet loaded");
        }
        return publicKey;
    }

    public boolean isReady() {
        return publicKey != null;
    }
}
