package com.schoolos.application.features.identity.ports;

import com.schoolos.domain.identity.Session;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Device sessions (not tenant-bound: plan 0003, R1). */
public interface SessionRepository {

    Optional<Session> findById(UUID sessionId);

    /** Reads and locks the session row, so two refreshes with one token cannot both win (review 1, M-1). */
    Optional<Session> lockById(UUID sessionId);

    void create(Session session);

    void update(Session session);

    /** Ends every open session of the account at once, on every device (spec 0004, Req. 8). */
    void revokeAllForUser(UUID userId, Instant now);
}
