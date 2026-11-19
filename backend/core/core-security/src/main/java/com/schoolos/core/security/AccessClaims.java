package com.schoolos.core.security;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/** What a short-lived access token asserts. The tenant comes from here, never from the request (BR-1). */
public record AccessClaims(UUID userId, UUID tenantId, UUID sessionId, Set<String> roles, Instant expiresAt) {

    public AccessClaims {
        roles = Set.copyOf(roles);
    }
}
