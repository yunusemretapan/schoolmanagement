package com.schoolos.application.features.identity.rules;

import com.schoolos.domain.identity.LoginIdentity;
import java.time.Duration;
import java.time.Instant;

/**
 * Lockout (spec 0003, Req. 5): after 5 failed attempts in a row the account is locked for 15 minutes; while locked,
 * even the right password is refused (and attempts are not counted). A success resets the counter; an expired lock
 * starts a fresh count.
 */
public final class LoginAttemptPolicy {

    public static final int MAX_FAILED_ATTEMPTS = 5;
    public static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private LoginAttemptPolicy() {}

    public static boolean isLocked(LoginIdentity identity, Instant now) {
        return identity.lockedUntil() != null && now.isBefore(identity.lockedUntil());
    }

    public static LoginIdentity afterFailure(LoginIdentity identity, Instant now) {
        int previous = identity.lockedUntil() == null ? identity.failedAttempts() : 0;
        int failed = previous + 1;
        if (failed >= MAX_FAILED_ATTEMPTS) {
            return identity.withAttempts(0, now.plus(LOCK_DURATION));
        }
        return identity.withAttempts(failed, null);
    }

    public static LoginIdentity afterSuccess(LoginIdentity identity) {
        return identity.withAttempts(0, null);
    }
}
