package com.schoolos.domain.identity;

import java.time.Instant;
import java.util.UUID;

/**
 * One device's session: a rotating refresh chain. Only the hash of the current refresh secret is kept; presenting
 * any other secret for this session means an old one was reused, and the whole chain is revoked (spec 0003, Req. 6).
 */
public record Session(
        UUID id,
        UUID userId,
        UUID tenantId,
        ClientType clientType,
        String tokenHash,
        Instant createdAt,
        Instant lastUsedAt,
        Instant revokedAt) {

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public Session rotated(String newTokenHash, Instant now) {
        return new Session(id, userId, tenantId, clientType, newTokenHash, createdAt, now, revokedAt);
    }

    public Session revoked(Instant now) {
        return new Session(id, userId, tenantId, clientType, tokenHash, createdAt, lastUsedAt, now);
    }
}
