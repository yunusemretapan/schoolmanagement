package com.schoolos.application.features.identity.rules;

import com.schoolos.application.features.identity.ports.SessionRepository;
import com.schoolos.core.security.SessionValidator;
import com.schoolos.domain.identity.Session;
import java.util.UUID;

/** An access token only counts while its session is open: logout takes effect at once (spec 0003, AC-9). */
public final class SessionActivityChecker implements SessionValidator {

    private final SessionRepository sessions;

    public SessionActivityChecker(SessionRepository sessions) {
        this.sessions = sessions;
    }

    @Override
    public boolean isActive(UUID sessionId) {
        return sessions.findById(sessionId)
                .map(Session::isRevoked)
                .map(revoked -> !revoked)
                .orElse(false);
    }
}
