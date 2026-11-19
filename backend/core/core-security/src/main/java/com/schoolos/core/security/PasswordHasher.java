package com.schoolos.core.security;

/** One-way password hashing (docs/security.md: Argon2id). */
public interface PasswordHasher {

    String hash(String rawPassword);

    boolean matches(String rawPassword, String hash);
}
