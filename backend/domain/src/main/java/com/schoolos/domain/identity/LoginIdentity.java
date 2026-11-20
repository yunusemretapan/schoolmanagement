package com.schoolos.domain.identity;

import java.time.Instant;
import java.util.UUID;

/**
 * How a user signs in: normalized e-mail and/or phone (unique platform-wide), the password hash, the lockout state
 * (spec 0003, Req. 2–5) and the account status (spec 0004). An invited account has no password until it accepts its
 * invitation; an active one always has one.
 */
public record LoginIdentity(
        UUID userId,
        UUID tenantId,
        String email,
        String phone,
        String passwordHash,
        int failedAttempts,
        Instant lockedUntil,
        AccountStatus status) {

    public LoginIdentity {
        if (email == null && phone == null) {
            throw new IllegalArgumentException("A login identity needs an e-mail or a phone number");
        }
        if (status == null) {
            throw new IllegalArgumentException("A login identity needs a status");
        }
        if (status == AccountStatus.ACTIVE && passwordHash == null) {
            throw new IllegalArgumentException("An active account needs a password");
        }
    }

    /** An active account with a password (spec 0003 accounts). */
    public LoginIdentity(
            UUID userId,
            UUID tenantId,
            String email,
            String phone,
            String passwordHash,
            int failedAttempts,
            Instant lockedUntil) {
        this(userId, tenantId, email, phone, passwordHash, failedAttempts, lockedUntil, AccountStatus.ACTIVE);
    }

    /** A new invited account: no password yet (spec 0004, Req. 4). */
    public static LoginIdentity invited(UUID userId, UUID tenantId, String email, String phone) {
        return new LoginIdentity(userId, tenantId, email, phone, null, 0, null, AccountStatus.INVITED);
    }

    public boolean canSignIn() {
        return status == AccountStatus.ACTIVE;
    }

    public LoginIdentity withAttempts(int failed, Instant lockUntil) {
        return new LoginIdentity(userId, tenantId, email, phone, passwordHash, failed, lockUntil, status);
    }

    /** The invitation was accepted: the password is set and the account becomes active (spec 0004, Req. 6). */
    public LoginIdentity activatedWith(String newPasswordHash) {
        return new LoginIdentity(userId, tenantId, email, phone, newPasswordHash, 0, null, AccountStatus.ACTIVE);
    }

    public LoginIdentity deactivated() {
        return new LoginIdentity(
                userId, tenantId, email, phone, passwordHash, failedAttempts, lockedUntil, AccountStatus.INACTIVE);
    }

    /** Back from inactive: active with the old password, or invited again if it never had one (spec 0004, G5). */
    public LoginIdentity reactivated() {
        AccountStatus next = passwordHash == null ? AccountStatus.INVITED : AccountStatus.ACTIVE;
        return new LoginIdentity(userId, tenantId, email, phone, passwordHash, 0, null, next);
    }

    /** Only while invited (spec 0004, Req. 11); values are already normalized. */
    public LoginIdentity withIdentifiers(String newEmail, String newPhone) {
        return new LoginIdentity(
                userId, tenantId, newEmail, newPhone, passwordHash, failedAttempts, lockedUntil, status);
    }
}
