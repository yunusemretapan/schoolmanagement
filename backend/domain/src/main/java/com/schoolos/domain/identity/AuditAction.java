package com.schoolos.domain.identity;

/** What a staff-management audit event records (spec 0004, Req. 10). */
public enum AuditAction {
    STAFF_INVITED,
    INVITATION_RENEWED,
    INVITATION_ACCEPTED,
    ROLE_ADDED,
    ROLE_REMOVED,
    STAFF_DETAILS_CHANGED,
    STAFF_DEACTIVATED,
    STAFF_REACTIVATED
}
