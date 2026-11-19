package com.schoolos.application.features.identity.ports;

import com.schoolos.domain.identity.Session;
import java.util.Optional;
import java.util.UUID;

/** Device sessions (not tenant-bound: plan 0003, R1). */
public interface SessionRepository {

    Optional<Session> findById(UUID sessionId);

    /** Reads and locks the session row, so two refreshes with one token cannot both win (review 1, M-1). */
    Optional<Session> lockById(UUID sessionId);

    void create(Session session);

    void update(Session session);
}
