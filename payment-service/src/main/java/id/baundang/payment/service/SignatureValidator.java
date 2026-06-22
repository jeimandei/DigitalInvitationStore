package id.baundang.payment.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class SignatureValidator {

    private final String serverKey;

    public SignatureValidator(@Value("${app.midtrans.server-key}") String serverKey) {
        this.serverKey = serverKey;
    }

    public boolean isValid(String orderId, String statusCode, String grossAmount, String signatureKey) {
        String raw = orderId + statusCode + grossAmount + serverKey;
        String expected = sha512(raw);
        return expected.equalsIgnoreCase(signatureKey);
    }

    private String sha512(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-512 unavailable", e);
        }
    }
}
