package com.schoolos.application.features.identity.rules;

import com.schoolos.application.features.identity.ports.AuditLog;
import com.schoolos.core.application.audit.AuditEntry;
import com.schoolos.domain.identity.AuditEvent;

/**
 * The identity module's audit records (spec 0004) written into the shared audit log (spec 0005, plan R1): the target
 * is a user account, named by id only.
 */
public final class IdentityAuditLog implements AuditLog {

    private final com.schoolos.core.application.audit.AuditLog shared;

    public IdentityAuditLog(com.schoolos.core.application.audit.AuditLog shared) {
        this.shared = shared;
    }

    @Override
    public void append(AuditEvent event) {
        shared.append(toEntry(event));
    }

    public static AuditEntry toEntry(AuditEvent event) {
        return new AuditEntry(
                event.id(),
                event.tenantId(),
                event.actorUserId(),
                AuditEntry.USER,
                event.targetUserId(),
                null,
                event.action().name(),
                event.detail(),
                event.at());
    }
}
