package com.schoolos.application.features.identity.ports;

import java.security.SecureRandom;
import java.util.Base64;

/** New refresh secrets. Injected so tests can predict them. */
@FunctionalInterface
public interface SecretGenerator {

    String newSecret();

    /** 32 random bytes from {@link SecureRandom}, base64url without padding. */
    static SecretGenerator secureRandom() {
        SecureRandom random = new SecureRandom();
        return () -> {
            byte[] bytes = new byte[32];
            random.nextBytes(bytes);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        };
    }
}
