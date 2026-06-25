package id.baundang.auth.controller;

import id.baundang.auth.dto.LoginRequest;
import id.baundang.auth.dto.OrderTokenRequest;
import id.baundang.auth.dto.RefreshRequest;
import id.baundang.auth.dto.RegisterRequest;
import id.baundang.auth.dto.TokenResponse;
import id.baundang.auth.service.AuthService;
import id.baundang.auth.service.JwtService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService  jwtService;

    @Value("${app.admin-seed-key:}")
    private String adminSeedKey;

    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService  = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @PostMapping("/register")
    public ResponseEntity<TokenResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(req));
    }

    @GetMapping(value = "/public-key", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> publicKey() {
        return ResponseEntity.ok(jwtService.publicKeyPem());
    }

    @PostMapping("/token/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest req) {
        return ResponseEntity.ok(authService.refresh(req));
    }

    @PostMapping("/order-token")
    public ResponseEntity<String> orderToken(@Valid @RequestBody OrderTokenRequest req) {
        return ResponseEntity.ok(authService.issueOrderToken(req));
    }

    @PostMapping("/register-admin")
    public ResponseEntity<TokenResponse> registerAdmin(
            @RequestHeader("X-Admin-Seed-Key") String key,
            @Valid @RequestBody RegisterRequest req) {
        if (adminSeedKey.isBlank() || !adminSeedKey.equals(key)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid seed key");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerAdmin(req));
    }
}
