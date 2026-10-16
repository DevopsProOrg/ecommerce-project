package com.shop.ecommerce.service;

import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Service
public class PasswordService {

    private final PasswordEncoder passwordEncoder;

    public PasswordService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public String hash(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }

        return passwordEncoder.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String encodedPassword) {
        if (encodedPassword == null) {
            return false;
        }

        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        if (encodedPassword.startsWith("$2a$") || encodedPassword.startsWith("$2b$") || encodedPassword.startsWith("$2y$")) {
            return passwordEncoder.matches(rawPassword, encodedPassword);
        }
        // Legacy SHA-256 and plaintext accounts are upgraded by AuthService after successful sign-in.
        try {
            if (MessageDigest.isEqual(rawPassword.getBytes(StandardCharsets.UTF_8), encodedPassword.getBytes(StandardCharsets.UTF_8))) {
                return true;
            }
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] legacy = Base64.getDecoder().decode(encodedPassword);
            byte[] candidate = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            return MessageDigest.isEqual(legacy, candidate);
        } catch (IllegalArgumentException | NoSuchAlgorithmException ex) {
            return false;
        }
    }

    public boolean needsUpgrade(String encodedPassword) {
        return encodedPassword == null || !(encodedPassword.startsWith("$2a$") || encodedPassword.startsWith("$2b$") || encodedPassword.startsWith("$2y$"));
    }
}
