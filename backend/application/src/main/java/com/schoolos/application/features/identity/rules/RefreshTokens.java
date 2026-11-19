package com.schoolos.application.features.identity.rules;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

/**
 * A refresh token is {@code <sessionId>.<secret>}. Only the SHA-256 of the secret is stored, and it is compared in
 * constant time.
 */
public final class RefreshTokens {

    public record Parsed(UUID sessionId, String secret) {}

    private RefreshTokens() {}

    public static String format(UUID sessionId, String secret) {
        return sessionId + "." + secret;
    }

    public static Optional<Parsed> parse(String token) {
        if (token == null) {
            return Optional.empty();
        }
        int dot = token.indexOf('.');
        if (dot <= 0 || dot == token.length() - 1) {
            return Optional.empty();
        }
        try {
            return Optional.of(new Parsed(UUID.fromString(token.substring(0, dot)), token.substring(dot + 1)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public static String hash(String secret) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by every Java platform", e);
        }
    }

    public static boolean matches(String secret, String storedHash) {
        return MessageDigest.isEqual(
                hash(secret).getBytes(StandardCharsets.US_ASCII), storedHash.getBytes(StandardCharsets.US_ASCII));
    }
}
