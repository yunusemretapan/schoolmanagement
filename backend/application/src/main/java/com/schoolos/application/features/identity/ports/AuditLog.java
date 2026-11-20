package com.schoolos.application.features.identity.ports;

import com.schoolos.domain.identity.AuditEvent;

/**
 * The append-only audit log (spec 0004, Req. 10). Append is the only operation on purpose: nothing in the
 * application can change or remove a record; the database refuses it too (V2__staff_management.sql).
 */
@FunctionalInterface
public interface AuditLog {

    void append(AuditEvent event);
}
