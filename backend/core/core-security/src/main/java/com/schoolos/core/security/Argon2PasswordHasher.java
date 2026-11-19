package com.schoolos.core.security;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

/** Argon2id with Spring Security's recommended parameters (salt 16 B, hash 32 B, m=16 MiB, t=2). */
public final class Argon2PasswordHasher implements PasswordHasher {

    private final Argon2PasswordEncoder encoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();

    @Override
    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String hash) {
        return encoder.matches(rawPassword, hash);
    }
}
