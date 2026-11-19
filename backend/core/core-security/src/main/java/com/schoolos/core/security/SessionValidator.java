package com.schoolos.core.security;

import java.util.UUID;

/** Is the session behind an access token still open? Logged-out sessions are refused at once (spec 0003, AC-9). */
@FunctionalInterface
public interface SessionValidator {

    boolean isActive(UUID sessionId);
}
