package com.schoolos.domain.identity;

import java.time.Instant;
import java.util.UUID;

/**
 * How a user signs in: normalized e-mail and/or phone (unique platform-wide), the password hash, and the lockout
 * state (spec 0003, Req. 2–5).
 */
public record LoginIdentity(
        UUID userId,
        UUID tenantId,
        String email,
        String phone,
        String passwordHash,
        int failedAttempts,
        Instant lockedUntil) {

    public LoginIdentity {
        if (email == null && phone == null) {
            throw new IllegalArgumentException("A login identity needs an e-mail or a phone number");
        }
    }

    public LoginIdentity withAttempts(int failed, Instant lockUntil) {
        return new LoginIdentity(userId, tenantId, email, phone, passwordHash, failed, lockUntil);
    }
}
