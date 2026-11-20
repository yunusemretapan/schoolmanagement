package com.schoolos.domain.identity;

import java.time.Instant;
import java.util.UUID;

/**
 * Davet: a one-time link that lets an invited account set its password (spec 0004, Req. 5). Only the hash of the
 * link's secret is kept. Usable until it expires, is used once, or is revoked (renewed, identifiers changed,
 * account deactivated).
 */
public record Invitation(
        UUID id,
        UUID userId,
        UUID tenantId,
        String tokenHash,
        Instant createdAt,
        Instant expiresAt,
        Instant usedAt,
        Instant revokedAt) {

    public boolean isUsable(Instant now) {
        return usedAt == null && revokedAt == null && now.isBefore(expiresAt);
    }

    public Invitation used(Instant now) {
        return new Invitation(id, userId, tenantId, tokenHash, createdAt, expiresAt, now, revokedAt);
    }

    public Invitation revoked(Instant now) {
        return new Invitation(id, userId, tenantId, tokenHash, createdAt, expiresAt, usedAt, now);
    }
}
