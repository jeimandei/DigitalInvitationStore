package id.baundang.auth.service;

import id.baundang.auth.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private static final String PRIVATE_KEY_FILE = "auth_rsa";
    private static final String PUBLIC_KEY_FILE  = "auth_rsa.pub";
    private static final int KEY_SIZE = 4096;

    @Value("${app.jwt.keys-dir}")
    private String keysDir;

    @Value("${app.jwt.admin-expiry-hours:8}")
    private int adminExpiryHours;

    @Value("${app.jwt.buyer-expiry-hours:24}")
    private int buyerExpiryHours;

    @Value("${app.jwt.order-token-expiry-minutes:60}")
    private int orderTokenExpiryMinutes;

    private PrivateKey privateKey;
    private PublicKey  publicKey;

    @PostConstruct
    public void init() throws Exception {
        Path dir      = Path.of(keysDir);
        Path privPath = dir.resolve(PRIVATE_KEY_FILE);
        Path pubPath  = dir.resolve(PUBLIC_KEY_FILE);

        if (Files.exists(privPath) && Files.exists(pubPath)) {
            log.info("Loading RSA key pair from {}", keysDir);
            privateKey = loadPrivateKey(privPath);
            publicKey  = loadPublicKey(pubPath);
        } else {
            log.info("Generating new RSA-{} key pair in {}", KEY_SIZE, keysDir);
            Files.createDirectories(dir);
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(KEY_SIZE, new SecureRandom());
            KeyPair pair = gen.generateKeyPair();
            privateKey = pair.getPrivate();
            publicKey  = pair.getPublic();
            Files.writeString(privPath, encodePem("PRIVATE KEY", privateKey.getEncoded()));
            Files.writeString(pubPath,  encodePem("PUBLIC KEY",  publicKey.getEncoded()));
            privPath.toFile().setReadable(false, false);
            privPath.toFile().setReadable(true, true);
        }
    }

    public String issueAccessToken(User user) {
        int expiryHours = user.getRole() == User.Role.ADMIN ? adminExpiryHours : buyerExpiryHours;
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(user.getId().toString())
                .claim("role", user.getRole().name())
                .claim("email", user.getEmail())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiryHours, ChronoUnit.HOURS)))
                .signWith(privateKey)
                .compact();
    }

    public String issueOrderToken(User user, UUID orderId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(user.getId().toString())
                .claim("role", user.getRole().name())
                .claim("order_id", orderId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(orderTokenExpiryMinutes, ChronoUnit.MINUTES)))
                .signWith(privateKey)
                .compact();
    }

    public Claims parseToken(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String publicKeyPem() {
        return encodePem("PUBLIC KEY", publicKey.getEncoded());
    }

    public long accessTokenExpirySeconds(User.Role role) {
        return role == User.Role.ADMIN
                ? adminExpiryHours * 3600L
                : buyerExpiryHours * 3600L;
    }

    private PrivateKey loadPrivateKey(Path path) throws Exception {
        byte[] decoded = decodePem(Files.readString(path), "PRIVATE KEY");
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(new PKCS8EncodedKeySpec(decoded));
    }

    private PublicKey loadPublicKey(Path path) throws Exception {
        byte[] decoded = decodePem(Files.readString(path), "PUBLIC KEY");
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(new X509EncodedKeySpec(decoded));
    }

    private String encodePem(String type, byte[] data) {
        String b64 = Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(data);
        return "-----BEGIN " + type + "-----\n" + b64 + "\n-----END " + type + "-----\n";
    }

    private byte[] decodePem(String pem, String type) {
        String stripped = pem
                .replace("-----BEGIN " + type + "-----", "")
                .replace("-----END "   + type + "-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(stripped);
    }
}
