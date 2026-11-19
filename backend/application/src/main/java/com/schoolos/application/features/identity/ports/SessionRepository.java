package com.schoolos.application.features.identity.ports;

import com.schoolos.domain.identity.Session;
import java.util.Optional;
import java.util.UUID;

/** Device sessions (not tenant-bound: plan 0003, R1). */
public interface SessionRepository {

    Optional<Session> findById(UUID sessionId);

    void create(Session session);

    void update(Session session);
}
