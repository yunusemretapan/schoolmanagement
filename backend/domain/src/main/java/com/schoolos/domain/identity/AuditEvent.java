package com.schoolos.domain.identity;

import java.time.Instant;
import java.util.UUID;

/**
 * One append-only audit record: who did what to whom, and when (spec 0004, Req. 10). {@code detail} names what
 * changed (a role, the changed field names), never a value such as a password, a link or contact data.
 */
public record AuditEvent(
        UUID id, UUID tenantId, UUID actorUserId, UUID targetUserId, AuditAction action, String detail, Instant at) {

    public AuditEvent {
        if (id == null
                || tenantId == null
                || actorUserId == null
                || targetUserId == null
                || action == null
                || at == null) {
            throw new IllegalArgumentException("An audit event needs who, whom, what and when");
        }
    }
}
