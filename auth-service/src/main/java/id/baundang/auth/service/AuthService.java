package id.baundang.auth.service;

import id.baundang.auth.domain.RefreshToken;
import id.baundang.auth.domain.User;
import id.baundang.auth.dto.LoginRequest;
import id.baundang.auth.dto.OrderTokenRequest;
import id.baundang.auth.dto.RefreshRequest;
import id.baundang.auth.dto.RegisterRequest;
import id.baundang.auth.dto.TokenResponse;
import id.baundang.auth.repository.RefreshTokenRepository;
import id.baundang.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepo;
    private final RefreshTokenRepository refreshRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.jwt.refresh-expiry-days:30}")
    private int refreshExpiryDays;

    public AuthService(UserRepository userRepo,
                       RefreshTokenRepository refreshRepo,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepo        = userRepo;
        this.refreshRepo     = refreshRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtService      = jwtService;
    }

    public TokenResponse login(LoginRequest req) {
        User user = userRepo.findByEmail(req.email().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        return buildTokenPair(user);
    }

    public TokenResponse register(RegisterRequest req) {
        if (userRepo.existsByEmail(req.email().toLowerCase())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        User user = new User(
                req.email().toLowerCase(),
                passwordEncoder.encode(req.password()),
                User.Role.BUYER
        );
        userRepo.save(user);

        return buildTokenPair(user);
    }

    public TokenResponse refresh(RefreshRequest req) {
        String hash = sha256Hex(req.refreshToken());

        RefreshToken stored = refreshRepo.findByTokenHash(hash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        if (stored.isRevoked() || stored.isExpired()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expired or revoked");
        }

        stored.revoke();
        return buildTokenPair(stored.getUser());
    }

    public String issueOrderToken(OrderTokenRequest req) {
        try {
            var claims = jwtService.parseToken(req.accessToken());
            UUID userId = UUID.fromString(claims.getSubject());

            User user = userRepo.findById(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

            return jwtService.issueOrderToken(user, req.orderId());
        } catch (io.jsonwebtoken.JwtException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid access token");
        }
    }

    private TokenResponse buildTokenPair(User user) {
        String accessToken = jwtService.issueAccessToken(user);
        String rawRefresh  = generateOpaqueToken();
        String refreshHash = sha256Hex(rawRefresh);

        refreshRepo.save(new RefreshToken(
                user,
                refreshHash,
                Instant.now().plus(refreshExpiryDays, ChronoUnit.DAYS)
        ));

        return new TokenResponse(accessToken, rawRefresh,
                jwtService.accessTokenExpirySeconds(user.getRole()));
    }

    private String generateOpaqueToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
