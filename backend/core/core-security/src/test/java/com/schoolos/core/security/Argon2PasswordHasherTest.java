package com.schoolos.core.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class Argon2PasswordHasherTest {

    private final Argon2PasswordHasher hasher = new Argon2PasswordHasher();

    @Test
    void hashesWithArgon2idAndVerifies() {
        String hash = hasher.hash("Dogru-Sifre-123");

        assertThat(hash).startsWith("$argon2id$").doesNotContain("Dogru-Sifre-123");
        assertThat(hasher.matches("Dogru-Sifre-123", hash)).isTrue();
        assertThat(hasher.matches("Yanlis-Sifre-123", hash)).isFalse();
    }

    @Test
    void saltsEachHash() {
        assertThat(hasher.hash("Ayni-Sifre-123")).isNotEqualTo(hasher.hash("Ayni-Sifre-123"));
    }
}
