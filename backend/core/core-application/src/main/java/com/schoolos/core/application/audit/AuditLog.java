package com.schoolos.core.application.audit;

/**
 * The append-only audit log every module writes to (spec 0004, Req. 10; spec 0005, Req. 10, plan R1). Append is the
 * only operation on purpose: nothing in the application can change or remove a record, and the database refuses it
 * too (V2__staff_management.sql trigger). The entry goes into the tenant the transaction is bound to (RLS WITH CHECK).
 */
@FunctionalInterface
public interface AuditLog {

    void append(AuditEntry entry);
}
