package com.schoolos.core.security;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Issues and verifies short-lived access tokens (docs/security.md: JWT). */
public interface TokenService {

    Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(15);

    String issue(UUID userId, UUID tenantId, UUID sessionId, Set<String> roles);

    /** Empty when the token is malformed, wrongly signed or expired. */
    Optional<AccessClaims> verify(String token);
}
