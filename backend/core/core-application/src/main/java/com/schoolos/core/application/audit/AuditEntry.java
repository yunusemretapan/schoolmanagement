package com.schoolos.core.application.audit;

import java.time.Instant;
import java.util.UUID;

/**
 * One append-only audit record, shared by every module (spec 0005, plan R1): who ({@code actorUserId}) did what
 * ({@code action}, {@code detail}) to which record ({@code targetType}, {@code targetId}, {@code targetName}), and
 * when. {@code detail} names what changed (field names, a role), never a secret or contact data. {@code targetName} is
 * only for non-personal records such as the academic structure; a person is named by id alone.
 */
public record AuditEntry(
        UUID id,
        UUID tenantId,
        UUID actorUserId,
        String targetType,
        UUID targetId,
        String targetName,
        String action,
        String detail,
        Instant at) {

    /** The target type of an audit entry about a user account. */
    public static final String USER = "USER";

    public AuditEntry {
        if (id == null
                || tenantId == null
                || actorUserId == null
                || targetType == null
                || targetType.isBlank()
                || targetId == null
                || action == null
                || action.isBlank()
                || at == null) {
            throw new IllegalArgumentException("An audit entry needs who, which record, what and when");
        }
        if (USER.equals(targetType) && targetName != null) {
            throw new IllegalArgumentException("A person is named by id only in the audit log");
        }
    }
}
