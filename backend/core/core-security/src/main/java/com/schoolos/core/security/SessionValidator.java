package com.schoolos.core.security;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * What the session behind an access token may do right now. Empty when the session was closed (spec 0003, AC-9) or
 * its account is no longer active (spec 0004, Req. 8); otherwise the account's current roles, read on every request,
 * so a removed role stops working at the next request (spec 0004, Req. 7; plan 0004, R2). The tenant comes from the
 * verified token.
 */
@FunctionalInterface
public interface SessionValidator {

    Optional<Set<String>> currentRoles(UUID sessionId, UUID tenantId);
}
